package at.aau.se2.skyjo.game.error

open class SkyjoGameException(message: String) : RuntimeException(message)

class InvalidGameSetupException(message: String) : SkyjoGameException(message)

class InvalidMoveException(message: String) : SkyjoGameException(message)

class GameNotStartedException(message: String) : SkyjoGameException(message)

class RoundAlreadyFinishedException(message: String) : SkyjoGameException(message)
