<h1 align="center"> Chat App </h1>

ChatApp es una aplicación de mensajería en tiempo real desarrollada en Kotlin utilizando Jetpack Compose como framework de interfaz de usuario.  
La aplicación permite a los usuarios registrarse o iniciar sesión, enviar y recibir mensajes en tiempo real y recibir notificaciones push cuando llegan nuevos mensajes.  
Está construida sobre servicios de Firebase e implementada siguiendo el patrón de arquitectura MVVM.

<h4 align="center">
:construction: Proyecto en construcción :construction:
</h4>

---

## Objetivo del proyecto

El propósito de esta aplicación es demostrar el desarrollo de una solución de chat moderna utilizando tecnologías actuales del ecosistema Android, aplicando buenas prácticas de arquitectura, gestión de estado y comunicación con servicios en la nube.

---

## Funcionalidades

### Autenticación
- Registro e inicio de sesión con correo y contraseña (Firebase Authentication)

### Mensajería en tiempo real
- Envío de mensajes
- Recepción automática de mensajes sin recargar la interfaz
- Almacenamiento en Firebase Realtime Database

### Notificaciones Push
- Envío de notificaciones mediante Cloud Messaging
- Recepción de notificaciones cuando la app está en segundo plano

### Interfaz de usuario
- Diseño completamente declarativo con Jetpack Compose
- Navegación entre pantallas con Navigation Compose
- Actualización automática de estado con ViewModel + State/Flow

---

## Arquitectura

Se utiliza el patrón de diseño MVVM (Model View Viewmodel) para mantener una separación clara entre interfaz, lógica de presentación y acceso a datos.
Esta estructura facilita la escalabilidad, la reutilización de lógica y la capacidad de testear componentes.

---

## Tecnologías y herramientas

**Lenguaje:** Kotlin  
**UI:** Jetpack Compose, Material 3  
**Arquitectura:** MVVM, ViewModel, StateFlow  
**Backend:** Firebase Authentication, Realtime Database, Cloud Messaging  
**Navegación:** Navigation Compose  
**Concurrencia:** Kotlin Coroutines / Flow  
**Control de versiones:** Git + GitHub  
**IDE:** Android Studio

---

## Vista previa de la aplicación



1. Pantalla de inicio de sesión / registro  
2. Lista o vista de chat  
3. Envío y recepción de mensajes en tiempo real  
4. Notificación push recibida



---

## Ejecución del proyecto

1. Clonar el repositorio.
2. Abrir el proyecto en Android Studio.
3. Crear un proyecto en Firebase y descargar `google-services.json`.
4. Colocar el archivo en la carpeta app
5. Habilitar los siguientes servicios en Firebase:
  - Authentication (Email/Password)
  - Realtime Database
  - Cloud Messaging
6. Sincronizar Gradle.
7. Ejecutar la aplicación en un dispositivo o emulador.

---

## Estructura de datos (Firebase Realtime Database)

Ejemplo de cómo se almacenan los mensajes:

messages/
chatId/
messageId/
senderId: String
text: String
timestamp: Long


---

## Buenas prácticas aplicadas

- Separación de responsabilidades (MVVM)
- Uso de ViewModel para gestión del ciclo de vida
- UI declarativa con Compose
- Gestión reactiva de estado con Flow
- Abstracción de acceso a datos con Repository
- Navegación desacoplada entre pantallas

---

## Mejoras planificadas

- Implementación de casos de uso en capa domain
- Manejo de estados avanzados (loading, error, vacío)
- Encriptación de mensajes
- Envío de archivos multimedia
- Chats grupales
- Tests unitarios para ViewModels y lógica de dominio

---

## Objetivo profesional

Este proyecto forma parte de mi portafolio como desarrollador Android, con el objetivo de mostrar experiencia en:
- Desarrollo de apps modernas con Compose
- Integración con servicios backend (Firebase)
- Aplicación de arquitectura limpia
- Buenas prácticas de código y escalabilidad

---

## Autor

[<img src="https://avatars.githubusercontent.com/u/84556441?s=400&u=9c2e1e6d95d361a45bb3fda23ebdf5b403e754ee&v=4" width=115><br><sub>Yeray Yas</sub>](https://github.com/yeray-yas)
:---:
Android Developer  



