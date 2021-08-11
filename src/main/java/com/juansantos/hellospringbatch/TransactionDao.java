package com.juansantos.hellospringbatch;

import java.util.List;

public interface TransactionDao {
    List<Transaction> getTransactionsByAccountNumber(String accountNumber);
}
