package com.mysofttechnology.homeautomation.utils

interface ExceptionListener {
    fun uncaughtException(thread: Thread, throwable: Throwable)
}