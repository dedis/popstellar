package com.github.dedis.popstellar.utility;

import com.github.dedis.popstellar.ui.home.ConnectingActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;

public class Constants {

  /** The extra key given by the home activity to LaoDetailActivity and Connecting Activity */
  public static final String LAO_ID_EXTRA = "lao_id";

  /** The extra key given by the LaoDetailActivity activity to DigitalCashActivity */
  public static final String LAO_NAME = "lao_name";

  /** The extra key given by the home LaoDetailActivity to DigitalCashActivity */
  public static final String ROLL_CALL_ID = "roll_call_id";

  /** The extra key given to the LaoDetailActivity when opened */
  public static final String FRAGMENT_TO_OPEN_EXTRA = "fragment_to_open";

  /** The extra value given to the LaoDetailActivity when lao_detail are to be opened */
  public static final String LAO_DETAIL_EXTRA = "lao_detail";

  /** The extra value given to the LaoDetailActivity when the wallet content is to be opened */
  public static final String CONTENT_WALLET_EXTRA = "content_wallet";

  /** The extra value given to the RollCallFragment when opened */
  public static final String RC_PK_EXTRA = "pk";

  /** The extra key given to indicate the activity to open for {@link ConnectingActivity} */
  public static final String ACTIVITY_TO_OPEN_EXTRA = "activity_to_open";

  /** The extra value given to {@link ConnectingActivity} to open {@link HomeActivity} */
  public static final String HOME_EXTRA = "home";

  /** Using the Resources class constant would require a min API of 29 */
  public static final int ID_NULL = 0;

  public static final float DISABLED_ALPHA = 0.4f;

  public static final float ENABLED_ALPHA = 1.0f;
}
