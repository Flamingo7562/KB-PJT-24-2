package com.gighub.wallet.service;

import com.gighub.wallet.service.command.EscrowHoldCommand;

public interface EscrowService {
    void hold(EscrowHoldCommand command);
}
