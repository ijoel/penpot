use skia_safe::gpu::{self, gl::FramebufferInfo, DirectContext};
use std::boxed::Box;

use skia_safe as skia;

extern "C" {
    pub fn emscripten_GetProcAddress(
        name: *const ::std::os::raw::c_char,
    ) -> *const ::std::os::raw::c_void;
}

struct GpuState {
    context: DirectContext,
    framebuffer_info: FramebufferInfo,
}

/// This struct holds the state of the Rust application between JS calls.
///
/// It is created by [init] and passed to the other exported functions. Note that rust-skia data
/// structures are not thread safe, so a state must not be shared between different Web Workers.
pub struct State {
    gpu_state: GpuState,
    surface: skia::Surface,
}

impl State {
    fn new(gpu_state: GpuState, surface: skia::Surface) -> Self {
        State { gpu_state, surface }
    }

    fn set_surface(&mut self, surface: skia::Surface) {
        self.surface = surface;
    }
}

fn init_gl() {
    unsafe {
        gl::load_with(|addr| {
            let addr = std::ffi::CString::new(addr).unwrap();
            emscripten_GetProcAddress(addr.into_raw() as *const _) as *const _
        });
    }
}

/// This needs to be done once per WebGL context.
fn create_gpu_state() -> GpuState {
    let interface = skia_safe::gpu::gl::Interface::new_native().unwrap();
    let context = skia_safe::gpu::direct_contexts::make_gl(interface, None).unwrap();
    let framebuffer_info = {
        let mut fboid: gl::types::GLint = 0;
        unsafe { gl::GetIntegerv(gl::FRAMEBUFFER_BINDING, &mut fboid) };

        FramebufferInfo {
            fboid: fboid.try_into().unwrap(),
            format: skia_safe::gpu::gl::Format::RGBA8.into(),
            protected: skia_safe::gpu::Protected::No,
        }
    };

    GpuState {
        context,
        framebuffer_info,
    }
}

/// Create the Skia surface that will be used for rendering.
fn create_surface(gpu_state: &mut GpuState, width: i32, height: i32) -> skia::Surface {
    let backend_render_target =
        gpu::backend_render_targets::make_gl((width, height), 1, 8, gpu_state.framebuffer_info);

    gpu::surfaces::wrap_backend_render_target(
        &mut gpu_state.context,
        &backend_render_target,
        skia_safe::gpu::SurfaceOrigin::BottomLeft,
        skia_safe::ColorType::RGBA8888,
        None,
        None,
    )
    .unwrap()
}

fn render_rect(surface: &mut skia::Surface, rect: skia::Rect, color: skia::Color) {
    let mut paint = skia::Paint::default();
    paint.set_style(skia::PaintStyle::Fill);
    paint.set_color(color);
    paint.set_anti_alias(true);
    surface.canvas().draw_rect(rect, &paint);
}

/// This is called from JS after the WebGL context has been created.
#[no_mangle]
pub extern "C" fn init(width: i32, height: i32) -> Box<State> {
    let mut gpu_state = create_gpu_state();
    let surface = create_surface(&mut gpu_state, width, height);

    let state = State::new(gpu_state, surface);

    Box::new(state)
}

/// This is called from JS when the window is resized.
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn resize_surface(state: *mut State, width: i32, height: i32) {
    let state = unsafe { state.as_mut() }.expect("got an invalid state pointer");
    let surface = create_surface(&mut state.gpu_state, width, height);
    state.set_surface(surface);
}

/// Draws a rect at the specified coordinates with the give ncolor
/// # Safety
#[no_mangle]
pub unsafe extern "C" fn draw_rect(state: *mut State, x1: f32, y1: f32, x2: f32, y2: f32) {
    let state = unsafe { state.as_mut() }.expect("got an invalid state pointer");
    let r = skia::Rect::new(x1, y1, x2, y2);
    render_rect(&mut state.surface, r, skia::Color::RED);
}

#[no_mangle]
pub unsafe extern "C" fn flush(state: *mut State) {
    let state = unsafe { state.as_mut() }.expect("got an invalid state pointer");
    state
        .gpu_state
        .context
        .flush_and_submit_surface(&mut state.surface, None);
}

#[no_mangle]
pub unsafe extern "C" fn translate(state: *mut State, dx: f32, dy: f32) {
    (*state).surface.canvas().translate((dx, dy));
}

#[no_mangle]
pub unsafe extern "C" fn scale(state: *mut State, sx: f32, sy: f32) {
    (*state).surface.canvas().scale((sx, sy));
}

#[no_mangle]
pub unsafe extern "C" fn reset_canvas(state: *mut State) {
    let state = unsafe { state.as_mut() }.expect("got an invalid state pointer");
    state.surface.canvas().clear(skia_safe::Color::TRANSPARENT);
    state.surface.canvas().reset_matrix();
    flush(state);
}

fn main() {
    init_gl();
}
