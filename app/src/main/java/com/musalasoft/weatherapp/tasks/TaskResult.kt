package com.musalasoft.weatherapp.tasks

enum class TaskResult {
    SUCCESS,
    HTTP_ERROR,
    IO_EXCEPTION,
    TOO_MANY_REQUESTS,
    INVALID_API_KEY
}
