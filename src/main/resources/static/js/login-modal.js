// Toggle + validación del modal de login.
// Se referencia igual en cualquier página que tenga el modal #modal-login en html
document.addEventListener("DOMContentLoaded", function () {
  const modal = document.getElementById("modal-login");
  const btnAbrir = document.getElementById("btn-abrir-login");
  const btnCerrar = document.getElementById("btn-cerrar-login");

  btnAbrir?.addEventListener("click", () => modal.classList.remove("hidden"));
  btnCerrar?.addEventListener("click", () => modal.classList.add("hidden"));
  modal?.addEventListener("click", (e) => {
    if (e.target === modal) modal.classList.add("hidden");
  });

  const formLogin = document.getElementById("form-login");
  const emailInput = document.getElementById("login-email");
  const passwordInput = document.getElementById("login-password");
  const errorEmail = document.querySelector('[data-error-for="login-email"]');
  const errorPassword = document.querySelector(
    '[data-error-for="login-password"]',
  );

  formLogin?.addEventListener("submit", (e) => {
    let esValido = true;
    [emailInput, passwordInput].forEach((input) => {
      if (input) input.classList.remove("input-error");
    });
    [errorEmail, errorPassword].forEach((msg) => {
      if (msg) msg.classList.add("hidden");
    });

    if (!emailInput?.value.trim()) {
      e.preventDefault();
      emailInput?.classList.add("input-error");
      errorEmail?.classList.remove("hidden");
      esValido = false;
    }
    if (!passwordInput?.value.trim()) {
      e.preventDefault();
      passwordInput?.classList.add("input-error");
      errorPassword?.classList.remove("hidden");
      esValido = false;
    }

    if (!esValido) {
      e.preventDefault();
      return;
    }

    // Si es válido, se envía el formulario al backend (POST /login)
  });

  // Limpia el error apenas el usuario empieza a corregir
  [
    [emailInput, errorEmail],
    [passwordInput, errorPassword],
  ].forEach(([input, msg]) => {
    input?.addEventListener("input", () => {
      input.classList.remove("input-error");
      msg?.classList.add("hidden");
    });
  });
});
