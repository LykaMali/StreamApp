package com.mali.streamapp.utils

import com.mali.streamapp.models.CallModel

open interface CallListener {
    open fun onLocalCallSended(callModel: CallModel)
    open fun onRemoteCallReceived(callModel: CallModel)
    open fun onRemoteUserDenied(callModel: CallModel)
    open fun onRemoteUserApproveCall(callModel: CallModel)
    open fun onCallTimeOut(callModel: CallModel)
}