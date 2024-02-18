package com.musalasoft.weatherapp.tasks

class TaskOutput {
    // Indicates result of parsing server response
    var parseResult: ParseResult? = null

    // Indicates result of background task
    var taskResult: TaskResult? = null

    // Error caused unsuccessful result
    var taskError: Throwable? = null
}
