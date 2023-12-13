package lila.game

private[game] case class Metadata(
    source: Option[Source],
    tournamentId: Option[String],
    swissId: Option[String],
    simulId: Option[String],
    analysed: Boolean
)
