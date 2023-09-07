package com.jumbo.trus.dto.receivedfine.response.get;

import java.util.List;

public interface IResponseMaker {

    void addFines(int fines);
    void addFineAmount(int fineAmount);
    void addMatchesCount(int count);
    void addPlayersCount(int count);
    void addFineList(List<?> fineList);
}
