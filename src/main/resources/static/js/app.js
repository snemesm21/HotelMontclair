
let habitacionSeleccionada = null;

let reservacionesHotel = JSON.parse(localStorage.getItem("montclair_reservas")) || [];

function guardarEnStorage() {
    localStorage.setItem("montclair_reservas", JSON.stringify(reservacionesHotel));
}

function abrirModalReserva(nombre, precio) {
    habitacionSeleccionada = { nombre, precio };

    document.getElementById("modal-titulo-habitacion").textContent = nombre;
    document.getElementById("modal-precio-noche").textContent = `Tarifa: $${precio.toFixed(2)} € / noche`;

    document.getElementById("modal-check-in").value = "2027-05-24";
    document.getElementById("modal-check-out").value = "2027-05-26";

    const modal = document.getElementById("modal-reserva");
    if (modal) {
        modal.classList.remove("hidden");
        modal.classList.add("flex");
    }
}

function cerrarModalReserva() {
    const modal = document.getElementById("modal-reserva");
    if (modal) {
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    }
}

document.addEventListener("DOMContentLoaded", () => {

    const formDisponibilidad = document.getElementById("form-disponibilidad");
    if (formDisponibilidad) {
        formDisponibilidad.addEventListener("submit", (e) => {
            e.preventDefault();
            alert("⚜ Fechas y disponibilidad confirmadas. Desplázate hacia abajo para elegir tu suite.");
            document.getElementById("habitaciones")?.scrollIntoView({ behavior: "smooth" });
        });
    }

    const btnCerrar = document.getElementById("btn-cerrar-modal");
    if (btnCerrar) {
        btnCerrar.addEventListener("click", cerrarModalReserva);
    }
    const modal = document.getElementById("modal-reserva");
    if (modal) {
        modal.addEventListener("click", (e) => {
            if (e.target.id === "modal-reserva") {
                cerrarModalReserva();
            }
        });
    }

    const formModal = document.getElementById("form-completar-reserva");
    if (formModal) {
        formModal.addEventListener("submit", (e) => {
            e.preventDefault();

            const nombre = document.getElementById("nombre-huesped").value;
            const correo = document.getElementById("correo-huesped").value;
            const checkIn = document.getElementById("modal-check-in").value;
            const checkOut = document.getElementById("modal-check-out").value;

            if (new Date(checkOut) <= new Date(checkIn)) {
                alert("La fecha de Check-Out debe ser posterior a la fecha de Check-In.");
                return;
            }

            const unDiaMilisegundos = 1000 * 60 * 60 * 24;
            const diffDias = Math.ceil((new Date(checkOut) - new Date(checkIn)) / unDiaMilisegundos);
            const costoTotal = (diffDias * habitacionSeleccionada.precio).toFixed(2);
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

            reservacionesHotel.push(nuevaReserva);
            guardarEnStorage();

            alert(
                `⚜ RESERVA CONFIRMADA - MONTCLAIR GRAND HOTEL ⚜\n\n` +
                `Huésped: ${nombre}\n` +
                `Suite: ${habitacionSeleccionada.nombre}\n` +
                `Estancia: ${diffDias} noche(s)\n` +
                `Total: $${costoTotal} €\n\n` +
                `¡Tu reserva ha sido guardada con éxito en el sistema!`
            );

            formModal.reset();
            cerrarModalReserva();
        });
    }
});
