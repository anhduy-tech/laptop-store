package com.lapstore.inventory.domain;

import com.lapstore.common.persistence.AuditableEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "stock_transfer_item")
public class StockTransferItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id")
    private StockTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_id")
    private SerialItem serial;

    @Column(name = "received_ok")
    private Boolean receivedOk;

    @Column(name = "receive_note", length = 255)
    private String receiveNote;

    public Long getId() { return id; }

    public StockTransfer getTransfer() { return transfer; }
    public void setTransfer(StockTransfer transfer) { this.transfer = transfer; }

    public SerialItem getSerial() { return serial; }
    public void setSerial(SerialItem serial) { this.serial = serial; }

    public Boolean getReceivedOk() { return receivedOk; }
    public void setReceivedOk(Boolean receivedOk) { this.receivedOk = receivedOk; }

    public String getReceiveNote() { return receiveNote; }
    public void setReceiveNote(String receiveNote) { this.receiveNote = receiveNote; }
}
