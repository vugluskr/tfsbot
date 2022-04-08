package model;

import services.BotApi;

import java.io.File;

/**
 * @author Denis Danilin | me@loslobos.ru
 * 04.04.2022 15:58
 * tfs ☭ sweat and blood
 */
public class MsgStruct {
    public String caption, body;
    public BotApi.Keyboard kbd;
    public TFile file;
    public File rawFile;
    public BotApi.ParseMode mode;
}
