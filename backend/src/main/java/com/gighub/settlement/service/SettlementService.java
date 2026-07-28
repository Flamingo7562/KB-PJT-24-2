package com.gighub.settlement.service;

import com.gighub.settlement.service.command.SettlementApproveCommand;
import com.gighub.settlement.service.result.SettlementResult;

public interface SettlementService {
    SettlementResult approve(SettlementApproveCommand command);
}
