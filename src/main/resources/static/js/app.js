// Copiado desde el proyecto original: js/app.js
// Variable global para almacenar los datos de la habitación elegida al abrir el modal
let habitacionSeleccionada = null;

// Cargar reservaciones previas almacenadas en LocalStorage
let reservacionesHotel = JSON.parse(localStorage.getItem("montclair_reservas")) || [];

// Guardar el array de reservaciones en LocalStorage
function guardarEnStorage() {
    localStorage.setItem("montclair_reservas", JSON.stringify(reservacionesHotel));
}

// Abrir la ventana modal flotante inyectando los datos de la suite elegida
function abrirModalReserva(nombre, precio) {
    habitacionSeleccionada = { nombre, precio };

    // Inyectar datos dinámicos en el encabezado del modal flotante
    document.getElementById("modal-titulo-habitacion").textContent = nombre;
    document.getElementById("modal-precio-noche").textContent = `Tarifa: $${precio.toFixed(2)} € / noche`;

    // Asignar fechas iniciales por defecto en el formulario flotante (formato YYYY-MM-DD)
    document.getElementById("modal-check-in").value = "2027-05-24";
    document.getElementById("modal-check-out").value = "2027-05-26";

    // Mostrar el modal removiendo la clase 'hidden' y añadiendo 'flex'
    const modal = document.getElementById("modal-reserva");
    if (modal) {
        modal.classList.remove("hidden");
        modal.classList.add("flex");
    }
}

// Cerrar la ventana modal flotante
function cerrarModalReserva() {
    const modal = document.getElementById("modal-reserva");
    if (modal) {
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }
}

// Inicialización de eventos al cargar el DOM de la página
document.addEventListener("DOMContentLoaded", () => {
    
    // 1. Envío de la barra de disponibilidad superior (Efecto visual de confirmación)
    const formDisponibilidad = document.getElementById("form-disponibilidad");
    if (formDisponibilidad) {
        formDisponibilidad.addEventListener("submit", (e) => {
            e.preventDefault();
            alert("⚜ Fechas y disponibilidad confirmadas. Desplázate hacia abajo para elegir tu suite.");
            document.getElementById("habitaciones")?.scrollIntoView({ behavior: "smooth" });
        });
    }

    // 2. Cerrar el modal con el botón de la 'X'
    const btnCerrar = document.getElementById("btn-cerrar-modal");
    if (btnCerrar) {
        btnCerrar.addEventListener("click", cerrarModalReserva);
    }

    // 3. Cerrar el modal al hacer clic en el fondo oscuro exterior
    const modal = document.getElementById("modal-reserva");
    if (modal) {
        modal.addEventListener("click", (e) => {
            if (e.target.id === "modal-reserva") {
                cerrarModalReserva();
            }
        });
    }

    // 4. Procesar el envío de datos del formulario dentro del modal flotante
    const formModal = document.getElementById("form-completar-reserva");
    if (formModal) {
        formModal.addEventListener("submit", (e) => {
            e.preventDefault();

            const nombre = document.getElementById("nombre-huesped").value;
            const correo = document.getElementById("correo-huesped").value;
            const checkIn = document.getElementById("modal-check-in").value;
            const checkOut = document.getElementById("modal-check-out").value;

            // Validación lógica: la salida debe ser posterior a la entrada
            if (new Date(checkOut) <= new Date(checkIn)) {
                alert("La fecha de Check-Out debe ser posterior a la fecha de Check-In.");
                return;
            }

            // Calcular cantidad de noches y costo total exacto
            const unDiaMilisegundos = 1000 * 60 * 60 * 24;
            const diffDias = Math.ceil((new Date(checkOut) - new Date(checkIn)) / unDiaMilisegundos);
            const costoTotal = (diffDias * habitacionSeleccionada.precio).toFixed(2);

            // Crear la estructura del objeto de reservación
            const nuevaReserva = {
                id: Date.now(),
                habitacion: habitacionSeleccionada.nombre,
                tarifaNoche: habitacionSeleccionada.precio,
                nombreHuesped: nombre,
                correoHuesped: correo,
                checkIn: checkIn,
                checkOut: checkOut,
                noches: diffDias,
                total: costoTotal
            };

            // Insertar en la lista y persistir en LocalStorage del navegador
            reservacionesHotel.push(nuevaReserva);
            guardarEnStorage();

            // Mensaje elegante de confirmación al estilo del hotel
            alert(
                `⚜ RESERVA CONFIRMADA - MONTCLAIR GRAND HOTEL ⚜\n\n` +
                `Huésped: ${nombre}\n` +
                `Suite: ${habitacionSeleccionada.nombre}\n` +
                `Estancia: ${diffDias} noche(s)\n` +
                `Total: $${costoTotal} €\n\n` +
                `¡Tu reserva ha sido guardada con éxito en el sistema!`
            );

            // Resetear inputs del formulario y cerrar la interfaz flotante
            formModal.reset();
            cerrarModalReserva();
        });
    }
});
