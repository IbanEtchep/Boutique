package fr.iban.boutique.sql;

public interface Callback<V extends Object, T extends Throwable> {

    void call(V result, T thrown);

}

