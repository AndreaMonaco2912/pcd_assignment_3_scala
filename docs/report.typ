#import "@preview/lilaq:0.4.0" as lq

#align(center, text(18pt)[*Assignment 3 di Programmazione Concorrente e Distribuita 2025/2026*])
#align(center, text(12pt)[Mattia Ronchi, matr. 0001236997 \ Samorì Andrea matr. 0001235969 \ Andrea Monaco matr. 000])

= Analisi del problema
L'obiettivo è lo sviluppo di un softwere per il controllo di un sistema di allarme. Il sistema deve essere in grado di elaborare input esterni da due fonti: un tastierino numerico e i sensiori nella casa. Il primo viene utilizzato dall'utente per inserire il PIN e gli ultimi servono per rilevare: movimenti, aperture porte, ecc... È richiesta anche una funzione di partizionamento della casa, in modo da armare solo alcune zone con l'allarme.

== Descrizione degli stati
Il sistema si comporta in maniera differente in base allo stato in cui si trova. Gli stati possibili sono:
- Disarmed: I sensori vengono ignorati.
- Exit Delay: Avviato dopo l'inserimento del PIN per armare il sistema, offre all'utente il tempo di uscire di casa senza far scattare l'allarme.
- Armed: Il sistema è attivo. Le rilevazioni dei sensori innescano la fase di allerta.
- Entry Delay: Una fase di tolleranza avviata da un sensore, in cui il sistema attende l'inserimento del PIN prima di far suonare la sirena.
- Alarm: Lo stato di emergenza, disattivabile solo tramite l'inserimento del PIN corretto.

= Aspetti rilevanti per la concorrenza

Il sistema deve essere in grado di ricevere segnali asincroni da diversi input, anche di natura diversa (PIN dal tastierino numerico da parte dell'utente, segnale da parte dei sensori e segnale da parte dei timer).

= Design della soluzione

Abbiamo optato per una soluzione tramite il modella ad attori fornito da Apache Pekko Typed in Scala, descrivendo il sistema tramite un macchina a stati finiti.

#figure(
  image("res/SmartAlarm.pdf", width: 75%),
  caption: [
    Diagramma degli stati del nostro sistema.
  ],
)

== Stato globale

Lo stato globale del sistema è incapsulato in un singolo attore (`SmarSmartAlarmSystem`). Infatti, il modello ad attori garantisce la sequenzialità dell'elaborazione dei messaggi, così da eliminare il problema di avere una memoria condivisa e dover utilizzare strumenti come mutex o lock per accedervi e modificarvici.

== Dinamicità del comportamento

Il sistema cambia comportamento in base ad input esterni. Questo è realizzato grazie alla possibilità fornita da Pekko di cambiare `Behavior` ad ogni messaggio ricevuto. Questo permette il passaggio da uno stato all'altro.

== Comunicazione asincrona

Tutte le comunicazioni avvengono tramite l'invio di messaggi all'attore. Per permettere questo, è stato definito un protocollo `Command` che contiene i possibili messaggi che l'attore può gestire.

== Gestione dei timer

Per non bloccare l'esecuzione quando dobbiamo aspettare un certo tempo (ad esempio quando abbiamo messo il PIN e abbiamo N secondi per uscire di casa), abbiamo utilizzato `Behaviors.withTimers`. Questo permette di dire all'attore di auto-inviarsi un messaggio (`ExitTimer` o `EnterTimer`) allo scadere del tempo richiesto.

== (Bonus) Divisione in zone

Abbiamo inserito una variabile `armedZones` allo stato dell'attore. Il messaggio `ArmingPin` riempie questa variabile con le zone specificate dall'utente. Poi, quando il sistema è in stato `armed`, tramite pattern matching capiamo  se il messaggio proveniva da un sensore di una zona armata oppure no.

```scala
case Command.SensorDetection(zone) if armedZones.contains(zone) =>
      ctx.log.info("A-Movement detected.")
```
