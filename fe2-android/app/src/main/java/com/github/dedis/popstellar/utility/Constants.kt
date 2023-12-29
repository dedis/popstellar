package com.github.dedis.popstellar.utility

object Constants {
  /** The extra key given by the home activity to LaoDetailActivity and Connecting Activity */
  const val LAO_ID_EXTRA = "lao_id"

  /** The extra key given by the LaoDetailActivity activity to DigitalCashActivity */
  const val LAO_NAME = "lao_name"
  const val WITNESSES = "witnesses"
  const val WITNESSING_FLAG_EXTRA = "isWitnessingEnabled"

  /** The extra key given to transmit a roll call id */
  const val ROLL_CALL_ID = "roll_call_id"

  /** The extra key given to transmit a roll call id */
  const val MEETING_ID = "meeting_id"

  /** The extra key given to the LaoDetailActivity when opened */
  const val FRAGMENT_TO_OPEN_EXTRA = "fragment_to_open"

  /** The extra value given to the LaoDetailActivity when lao_detail are to be opened */
  const val LAO_DETAIL_EXTRA = "lao_detail"

  /** The extra value given to the LaoDetailActivity when the wallet content is to be opened */
  const val CONTENT_WALLET_EXTRA = "content_wallet"

  /** The tab to open in LaoDetailActivity */
  const val TAB_EXTRA = "tab_extra"

  /** The extra key given to indicate the activity to open for [ConnectingActivity] */
  const val ACTIVITY_TO_OPEN_EXTRA = "activity_to_open"

  /** The extra value given to [ConnectingActivity] to open [HomeActivity] */
  const val HOME_EXTRA = "home"

  /**
   * The extra key given to indicate whether the [ConnectingActivity] is for creating or joining an
   * LAO
   */
  const val CONNECTION_PURPOSE_EXTRA = "connection_purpose"

  /** The extra value given to [ConnectingActivity] for joining an existing LAO */
  const val JOINING_EXTRA = "joining"

  /** The extra value given to [ConnectingActivity] for creating an LAO */
  const val CREATING_EXTRA = "creating"

  /** Using the Resources class constant would require a min API of 29 */
  const val ID_NULL = 0
  const val DISABLED_ALPHA = 0.2f
  const val ENABLED_ALPHA = 1.0f

  /** Standard size of the side of a displayed QR code */
  const val QR_SIDE = 800

  /** Number of milliseconds in a day */
  const val MS_IN_A_DAY = 1000 * 60 * 60 * 24L

  /** Orientation up along the x axis */
  const val ORIENTATION_UP = 0f

  /** Orientation down along the x axis */
  const val ORIENTATION_DOWN = 180f
}
