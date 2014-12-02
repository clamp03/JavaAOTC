package com.sun.corba.se.internal.orbutil.resources;

import java.util.ListResourceBundle;

public final class sunorb_de extends ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "bootstrap.exception", "Beim Speichern von Eigenschaften in Datei {0} wurde eine Ausnahme aufgefangen: Ausnahme {1}." },
            { "bootstrap.filenotfound", "Die Datei {0} wurde nicht gefunden." },
            { "bootstrap.filenotreadable", "Die Datei {0} kann nicht gelesen werden." },
            { "bootstrap.success", "Ein Port wird auf {0} eingestellt, und Dienste werden von {1} gelesen" },
            { "bootstrap.usage", "Syntax: {0} <Optionen> \n\nwobei folgende <Optionen> m\u00F6glich sind:\n  -ORBInitialPort        Erster Port (erforderlich)\n  -InitialServicesFile   Datei mit Liste von Anfangsdiensten (erforderlich)\n" },
            { "orbd.commfailure", "\nORBD konnte nicht gestartet werden, weil ORBinitialPort bereits verwendet wird" },
            { "orbd.internalexception", "ORBD konnte wegen einer internen Ausnahme nicht gestartet werden. \nM\u00F6gliche Ursachen: \n1. Der angegebene ORBInitialPort oder ORBActivationPort wird bereits verwendet \n2. Keine Berechtigung zum Schreiben von orb.db " },
            { "orbd.usage", "Syntax: {0} <Optionen> \n\nwobei folgende <Optionen> m\u00F6glich sind:\n  -port                  Aktivierungsport, an dem der ORBD gestartet werden sollte, Standardvorgabe 1049 (optional)\n  -defaultdb             Verzeichnis f\u00FCr ORBD-Dateien, Standardvorgabe \"./orb.db\" (optional)\n  -serverid              Server-ID f\u00FCr ORBD, Standardvorgabe 1 (optional)\n  -ORBInitialPort        Anfangsport (erforderlich)\n  -ORBInitialHost        Anf\u00E4nglicher Rechnername (erforderlich)\n" },
            { "pnameserv.success", "St\u00E4ndiger Namensserver erfolgreich hochgefahren" },
            { "servertool.appname", "\tAnwendungsname     - {0}" },
            { "servertool.args", "\tArgs      - {0}" },
            { "servertool.baddef", "Fehlerhafte Serverdefinition: {0}" },
            { "servertool.banner", "\n\nWillkommen beim Java IDL-Server-Tool \nBitte geben Sie an der Eingabeaufforderung Befehle ein. \n" },
            { "servertool.classpath", "\tKlassenpfad - {0}" },
            { "servertool.getserverid", "\n\tgetserverid [ -applicationName <Name> ] \n" },
            { "servertool.getserverid1", "\u00DCbergeben der Server-ID f\u00FCr einen Anwendungsnamen" },
            { "servertool.getserverid2", "\tServer-ID f\u00FCr Anwendungsname {0} ist {1}." },
            { "servertool.helddown", "\tServer ist au\u00DFer Betrieb." },
            { "servertool.help", "\thelp\n\tOR\n\thelp <Befehlsname>\n" },
            { "servertool.help1", "Hilfe anfordern" },
            { "servertool.list", "\n\tlist\n" },
            { "servertool.list1", "Auflisten aller registrierten Server" },
            { "servertool.list2", "\n\tServer-ID\tServer-Klassenname\t\tServer-Anwendung\n\t---------\t------------------\t\t----------------\n" },
            { "servertool.listactive", "\n\tlistactive" },
            { "servertool.listactive1", "Auflisten der gegenw\u00E4rtig aktiven Server" },
            { "servertool.listappnames", "\tlistappnames\n" },
            { "servertool.listappnames1", "Auflisten der gegenw\u00E4rtig definierten Anwendungsnamen" },
            { "servertool.listappnames2", "Gegenw\u00E4rtig definierte Serveranwendungsnamen:" },
            { "servertool.locate", "\n\tlocate [ -serverid <Server-ID> | -applicationName <Name> ] [ <-endpointType <Endpunkttyp> ] \n" },
            { "servertool.locate1", "Finden von Ports eines speziellen Typs bei einem registrierten Server" },
            { "servertool.locate2", "\n\n\tHostname {0} \n\n\t\tPort\t\tPorttyp\t\tORB-ID\n\t\t----\t\t-------\t\t------\n" },
            { "servertool.locateorb", "\n\tlocateperorb [ -serverid <Server-ID> | -applicationName <Name> ] [ -orbid <ORB-Name> ]\n" },
            { "servertool.locateorb1", "Finden von Ports eines speziellen ORBs bei einem registrierten Server" },
            { "servertool.locateorb2", "\n\n\tHostname {0} \n\n\t\tPort\t\tPorttyp\t\tORB-ID\n\t\t----\t\t-------\t\t------\n" },
            { "servertool.name", "\tName      - {0}" },
            { "servertool.nosuchorb", "\tUng\u00FCltige ORB" },
            { "servertool.nosuchserver", "\tDer Server wurde nicht gefunden." },
            { "servertool.orbidmap", "\tSyntax: orblist [ -serverid <Server-ID> | -applicationName <Name> ]\n" },
            { "servertool.orbidmap1", "Liste von ORB-Namen und ihren Zuordnungen" },
            { "servertool.orbidmap2", "\n\tORB-ID\t\tORB-Name\n\t------\t\t--------\n" },
            { "servertool.quit", "\n\tquit\n" },
            { "servertool.quit1", "Dieses Tool beenden" },
            { "servertool.register", "\n\n\tregister -server <Serverklassenname> \n\t         -applicationName <alternativer Servername> \n\t         -classpath <Klassenpfad f\u00FCr Server> \n\t         -args <Argumente f\u00FCr Server> \n\t         -vmargs <Argumente f\u00FCr Server Java VM>\n" },
            { "servertool.register1", "aktivierbaren Server registrieren" },
            { "servertool.register2", "\tServer registriert (serverid = {0})" },
            { "servertool.register3", "\tServer registriert, aber au\u00DFer Betrieb (serverid = {0})" },
            { "servertool.register4", "\tServer bereits registriert (serverid = {0})" },
            { "servertool.serverid", "\tServer-ID - {0}" },
            { "servertool.servernotrunning", "\tServer l\u00E4uft nicht." },
            { "servertool.serverup", "\tServer ist bereits in Betrieb." },
            { "servertool.shorthelp", "\n\n\tVerf\u00FCgbare Befehle: \n\t------------------- \n" },
            { "servertool.shutdown", "\n\tshutdown [ -serverid <Server-ID> | -applicationName <Name> ]\n" },
            { "servertool.shutdown1", "Herunterfahren eines registrierten Servers" },
            { "servertool.shutdown2", "\tServer erfolgreich heruntergefahren" },
            { "servertool.startserver", "\n\tstartup [ -serverid <Server-ID> | -applicationName <Name> ]\n" },
            { "servertool.startserver1", "Hochfahren eines registrierten Servers" },
            { "servertool.startserver2", "\tServer erfolgreich hochgefahren" },
            { "servertool.unregister", "\n\tunregister [ -serverid <Server-ID> | -applicationName <Name> ] \n" },
            { "servertool.unregister1", "Registrierung eines registrierten Servers l\u00F6schen" },
            { "servertool.unregister2", "\tServer-Registrierung gel\u00F6scht" },
            { "servertool.usage", "Syntax: {0} <Optionen> \n\nwobei folgende <Optionen> m\u00F6glich sind:\n  -ORBInitialPort        Anfangsport (erforderlich)\n  -ORBInitialHost        Anf\u00E4nglicher Rechnername (erforderlich)\n" },
            { "servertool.vmargs", "\tVmargs    - {0}" },
            { "tnameserv.exception", "Beim Hochfahren des Bootstrap-Dienstes auf Port {0} wurde eine Ausnahme aufgefangen." },
            { "tnameserv.hs1", "Anf\u00E4nglicher Namenskontext:\n{0}" },
            { "tnameserv.hs2", "\u00DCbergangsnamensserver: Port f\u00FCr anf\u00E4ngliche Objektreferenzen wird eingestellt auf: {0}" },
            { "tnameserv.hs3", "Bereit" },
            { "tnameserv.invalidhostoption", "ORBInitialHost ist keine g\u00FCltige Option f\u00FCr NameService" },
            { "tnameserv.orbinitialport0", "ORBInitialPort 0 ist keine g\u00FCltige Option f\u00FCr NameService" },
            { "tnameserv.usage", "Versuchen Sie, einen anderen Port mit den Befehlszeilenargumenten -ORBInitialPort <Portnummer> zu verwenden." },
        };
    }
}