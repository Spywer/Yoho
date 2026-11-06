package com.spywer.yoho.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import retrofit2.Response

data class AuthErrorDto(
    val message: String? = null,
    val code: String? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("statusCode") val statusCode: Int? = null,
)

object AuthErrors {
    fun fromResponse(response: Response<*>): String =
        parse(response.code(), response.errorBody()?.string())

    fun fromThrowable(error: Throwable, fallback: String): String {
        if (error is HttpException) {
            return parse(error.code(), error.response()?.errorBody()?.string())
        }
        return error.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun parse(httpCode: Int, rawBody: String?): String {
        val dto = rawBody
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching {
                    YohoGson.instance.fromJson(it, AuthErrorDto::class.java)
                }.getOrNull()
            }
        val code = dto?.code?.trim().orEmpty()
        val message = dto?.message?.trim().orEmpty()
        localize(code, message)?.let { return it }
        return when (httpCode) {
            400 -> "Проверьте введённые данные"
            401 -> "Неверный email или пароль"
            403 -> "Доступ запрещён"
            409 -> "Такой аккаунт уже существует"
            422 -> "Проверьте введённые данные"
            429 -> "Слишком много попыток. Подождите немного"
            in 500..599 -> "Сервер временно недоступен"
            else -> message.ifBlank { "Ошибка авторизации ($httpCode)" }
        }
    }

    private fun localize(code: String, message: String): String? {
        when (code.uppercase()) {
            "INVALID_EMAIL_OR_PASSWORD" -> return "Неверный email или пароль"
            "INVALID_PASSWORD", "WRONG_PASSWORD" -> return "Неверный пароль"
            "USER_NOT_FOUND" -> return "Пользователь не найден"
            "USER_ALREADY_EXISTS", "EMAIL_ALREADY_EXISTS", "USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL" ->
                return "Email уже зарегистрирован"
            "USERNAME_IS_ALREADY_TAKEN", "USERNAME_ALREADY_EXISTS" ->
                return "Логин уже занят"
            "INVALID_EMAIL", "EMAIL_NOT_VALID" -> return "Некорректный email"
            "PASSWORD_TOO_SHORT", "PASSWORD_TOO_WEAK", "WEAK_PASSWORD" ->
                return "Пароль слишком короткий (минимум 8 символов)"
            "INVALID_USERNAME", "USERNAME_TOO_SHORT" -> return "Некорректный логин"
            "BAD_REQUEST" -> return "Проверьте введённые данные"
            "UNAUTHORIZED" -> return "Нужно войти в аккаунт"
            "TOO_MANY_REQUESTS" -> return "Слишком много попыток. Подождите немного"
            "FAILED_TO_CREATE_USER" -> return "Не удалось создать аккаунт"
        }
        val lower = message.lowercase()
        return when {
            message.isBlank() -> null
            "invalid email or password" in lower -> "Неверный email или пароль"
            "user already exists" in lower || "email already" in lower ->
                "Email уже зарегистрирован"
            "username" in lower && ("taken" in lower || "exists" in lower) ->
                "Логин уже занят"
            "invalid email" in lower -> "Некорректный email"
            "password" in lower && ("short" in lower || "least" in lower || "weak" in lower) ->
                "Пароль слишком короткий (минимум 8 символов)"
            "missing" in lower -> "Заполните все поля"
            else -> message
        }
    }
}
