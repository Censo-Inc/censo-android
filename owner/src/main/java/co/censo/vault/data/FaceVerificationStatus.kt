package co.censo.vault.data

// this may change once Ievgen adds to API, but this roughly matches what was discussed
enum class FaceVerificatonStatus(val value: String) {
    NOT_ENROLLED("NotEnrolled"), // user associated with the device has no face enrolled
    ENROLLED ("NotAuthorized"), // user associated with the device has a face enrolled but not authorized for the device
    AUTHORIZED("Authorized")    // this device passed face authorization
}