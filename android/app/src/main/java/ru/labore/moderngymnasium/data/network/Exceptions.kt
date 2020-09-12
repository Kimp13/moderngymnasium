package ru.labore.moderngymnasium.data.network

import java.io.IOException

class ClientConnectionException : IOException()

class ClientErrorException(val errorCode: Int) : IOException()