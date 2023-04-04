package com.github.dedis.popstellar.utility;

import com.github.dedis.popstellar.ui.home.ConnectingActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;

import java.util.regex.Pattern;

public class Constants {

  /** The extra key given by the home activity to LaoDetailActivity and Connecting Activity */
  public static final String LAO_ID_EXTRA = "lao_id";

  /** The extra key given by the LaoDetailActivity activity to DigitalCashActivity */
  public static final String LAO_NAME = "lao_name";

  /** The extra key given to transmit a roll call id */
  public static final String ROLL_CALL_ID = "roll_call_id";

  /** The extra key given to the LaoDetailActivity when opened */
  public static final String FRAGMENT_TO_OPEN_EXTRA = "fragment_to_open";

  /** The extra value given to the LaoDetailActivity when lao_detail are to be opened */
  public static final String LAO_DETAIL_EXTRA = "lao_detail";

  /** The extra value given to the LaoDetailActivity when the wallet content is to be opened */
  public static final String CONTENT_WALLET_EXTRA = "content_wallet";

  /** The tab to open in LaoDetailActivity */
  public static final String TAB_EXTRA = "tab_extra";

  /** The extra key given to indicate the activity to open for {@link ConnectingActivity} */
  public static final String ACTIVITY_TO_OPEN_EXTRA = "activity_to_open";

  /** The extra value given to {@link ConnectingActivity} to open {@link HomeActivity} */
  public static final String HOME_EXTRA = "home";

  /**
   * The extra key given to indicate whether the {@link ConnectingActivity} is for creating or
   * joining an LAO
   */
  public static final String CONNECTION_PURPOSE_EXTRA = "connection_purpose";

  /** The extra value given to {@link ConnectingActivity} for joining an existing LAO */
  public static final String JOINING_EXTRA = "joining";

  /** The extra value given to {@link ConnectingActivity} for creating an LAO */
  public static final String CREATING_EXTRA = "creating";

  /** Using the Resources class constant would require a min API of 29 */
  public static final int ID_NULL = 0;

  public static final float DISABLED_ALPHA = 0.2f;

  public static final float ENABLED_ALPHA = 1.0f;

  /** Standard size of the side of a displayed QR code */
  public static final int QR_SIDE = 800;

  /** Number of milliseconds in a day */
  public static final long MS_IN_A_DAY = 1000 * 60 * 60 * 24L;

  /** URL-safe base64 pattern */
  public static final Pattern BASE64_PATTERN =
      Pattern.compile("^(?:[A-Za-z0-9-_]{4})*(?:[A-Za-z0-9-_]{2}==|[A-Za-z0-9-_]{3}=)?$");
}
