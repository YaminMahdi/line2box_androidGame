package com.diu.yk_games.line2box.model;

public class MsgStore
{
    public String playerId="";
    public String timeData="";
    public String nmData="";
    public String msgData="Blue";
    public String lvlData="1";
    public MsgStore(){}

    public MsgStore(String playerId, String timeData, String nmData, String msgData, String lvlData) {
        this.playerId = playerId;
        this.timeData = timeData;
        this.nmData = nmData;
        this.msgData = msgData;
        this.lvlData = lvlData;
    }
}
