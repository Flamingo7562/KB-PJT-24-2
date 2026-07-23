package com.gighub.wallet.service;

import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.command.EscrowReleaseCommand;

public interface EscrowService {
    void hold(EscrowHoldCommand command);
    void release(EscrowReleaseCommand command);
}