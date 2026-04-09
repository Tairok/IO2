package com.app.client.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Invoice {
    private final IntegerProperty    id            = new SimpleIntegerProperty();
    private final StringProperty     invoiceNumber = new SimpleStringProperty();
    private final DoubleProperty     amount        = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDate> issuedAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> dueDate  = new SimpleObjectProperty<>();
    private final StringProperty     status        = new SimpleStringProperty();
    private final IntegerProperty    userId        = new SimpleIntegerProperty();

    public Invoice(int id, String invoiceNumber, double amount,
                   LocalDate issuedAt, LocalDate dueDate,
                   String status, int userId) {
        this.id.set(id);
        this.invoiceNumber.set(invoiceNumber);
        this.amount.set(amount);
        this.issuedAt.set(issuedAt);
        this.dueDate.set(dueDate);
        this.status.set(status);
        this.userId.set(userId);
    }

    // — gettery
    public int       getId()            { return id.get(); }
    public String    getInvoiceNumber() { return invoiceNumber.get(); }
    public double    getAmount()        { return amount.get(); }
    public LocalDate getIssuedAt()      { return issuedAt.get(); }
    public LocalDate getDueDate()       { return dueDate.get(); }
    public String    getStatus()        { return status.get(); }
    public int       getUserId()        { return userId.get(); }

    // — settery
    public void setId(int id)                            { this.id.set(id); }
    public void setInvoiceNumber(String invoiceNumber)   { this.invoiceNumber.set(invoiceNumber); }
    public void setAmount(double amount)                 { this.amount.set(amount); }
    public void setIssuedAt(LocalDate issuedAt)          { this.issuedAt.set(issuedAt); }
    public void setDueDate(LocalDate dueDate)            { this.dueDate.set(dueDate); }
    public void setStatus(String status)                 { this.status.set(status); }
    public void setUserId(int userId)                    { this.userId.set(userId); }

    // — properties
    public IntegerProperty    idProperty()            { return id; }
    public StringProperty     invoiceNumberProperty() { return invoiceNumber; }
    public DoubleProperty     amountProperty()        { return amount; }
    public ObjectProperty<LocalDate> issuedAtProperty(){ return issuedAt; }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }
    public StringProperty     statusProperty()        { return status; }
    public IntegerProperty    userIdProperty()        { return userId; }
}
