package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.view.LaoView

/** Exception thrown when a rollcall is expected to be found in an LAO and none exist  */
class NoRollCallException(laoId: String) : KeyException(
    "No RollCall exist in the LAO : $laoId"
) {
    constructor(lao: Lao) : this(lao.id)
    constructor(laoView: LaoView) : this(laoView.id)

    override val userMessage: Int
        get() = R.string.no_rollcall_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}