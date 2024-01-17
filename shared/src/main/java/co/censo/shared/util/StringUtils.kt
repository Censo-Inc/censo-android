package co.censo.shared.util
fun String.isDigitsOnly(): Boolean {
    for (char in this) {
        if (!char.isDigit()) {
            return false
        }
    }
    return true
}