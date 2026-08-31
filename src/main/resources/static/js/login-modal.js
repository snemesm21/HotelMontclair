// Toggle + validación del modal de login.
// Se referencia igual en cualquier página que tenga el modal
// #modal-login en html
document.addEventListener("DOMContentLoaded", function () {
  const modal = document.getElementById("modal-login");
  const btnAbrir = document.getElementById("btn-abrir-login");
  const btnCerrar = document.getElementById("btn-cerrar-login");

  btnAbrir?.addEventListener("click", () => modal.classList.remove("hidden"));
  btnCerrar?.addEventListener("click", () => modal.classList.add("hidden"));
  modal?.addEventListener("click", (e) => {
    if (e.target === modal) modal.classList.add("hidden");
  });

  // TEMPORAL: todavía no existe el controller de POST /login
  const formLogin = document.getElementById("form-login");
  const emailInput = document.getElementById("login-email");
  const passwordInput = document.getElementById("login-password");
  const errorEmail = document.querySelector('[data-error-for="login-email"]');
  const errorPassword = document.querySelector(
    '[data-error-for="login-password"]',
  );

  formLogin?.addEventListener("submit", (e) => {
    e.preventDefault();

    let esValido = true;
    [emailInput, passwordInput].forEach((input) =>
      input.classList.remove("input-error"),
    );
    [errorEmail, errorPassword].forEach((msg) => msg.classList.add("hidden"));

    if (!emailInput.value.trim() || !emailInput.checkValidity()) {
      emailInput.classList.add("input-error");
      errorEmail.classList.remove("hidden");
      esValido = false;
    }
    if (!passwordInput.value.trim()) {
      passwordInput.classList.add("input-error");
      errorPassword.classList.remove("hidden");
      esValido = false;
    }

    if (!esValido) return;

    console.log(
      "Login visual listo. Falta conectar POST /login en el backend.",
    );
  });

  // Limpia el error apenas el usuario empieza a corregir
  [
    [emailInput, errorEmail],
    [passwordInput, errorPassword],
  ].forEach(([input, msg]) => {
    input?.addEventListener("input", () => {
      input.classList.remove("input-error");
      msg.classList.add("hidden");
    });
  });
});
