# Notifier
![Maven Central](https://img.shields.io/maven-central/v/com.github.tomtzook/notifier)
![Travis (.org) branch](https://img.shields.io/travis/tomtzook/Notifier/master.svg)
![GitHub](https://img.shields.io/github/license/tomtzook/Notifier.svg)

An easy-to-use generic event dispatching library for java. 

- [Usage](#usage)
  - [Defining Events and Listeners](#defining-events-and-listeners)
  - [Controllers](#controllers)
    - [Registering for Events](#registering-for-events)
    - [Firing Events](#firing-events)
    - [Creating a Controller](#creating-a-controller)
- [Adding to Project](#adding-to-project)
  - [Gradle](#gradle)

## Usage

### Defining Events and Listeners

Events are defined by the `Event` interface. Listeners are defined by the `Listener` interface. Both interfaces have no method requirement, and only serve to identify events and listeners by type.

An event class should define information on the event which has occurred. For example, an event which describes a character being killed, may define which character is being, and who is the killer:
```Java
class DeathEvent implements Event {
    public Character character;
    public Character killer;
}
```
It is possible to define an heirarchy for events, such as a super class for `DeathEvent` which is meant for general character events:
```Java
class CharacterEvent implements Event {
   public Character character;
}

class DeathEvent extends CharacterEvent {
    public Character killer;
}
```

A listener interface should define callback methods which should be invoked to handle the event occurring. This callback method should receive the event which it expects and should handle. An callback method signature should return `void`, throw no exceptions, and receive an instance of the handled event. Following the earlier example, a listener for the death event of a character:
```Java
interface CharacterStateListener extends Listener {
  void onDeath(DeathEvent e);
}
```
A listener interface may define multiple callback methods, to handle different events. For example, a callback to handle an event of a character changing stance:
```Java
interface CharacterStateListener extends Listener {
  void onDeath(DeathEvent e);
  void onStanceChange(StanceChangeEvent e);
}
```
### Controllers

The `EventController` class is used for registering and firing events. 

#### Registering for Events

Registering for events is done via `EventController.registerListener`:
```Java
eventController.registerListener(new LoggingCharacterStateListener());
```

It is also possible to register a listener with a call filter:
```Java
Predicate<Event> filter = ...;
eventController.registerListener(new LoggingCharacterStateListener(), filter);
```
Where `filter.test(event)` should return `true` for any `event` that should be handled by this listener. 
Since `Event` is just a general interface, the `filter` would probably need to check the type of event first:
```Java
Predicate<Event> filter = (e)-> {
  return e instanceof CharacterEvent && ((CharacterEvent)e).character.equals(mainCharacter);
}
```

#### Firing Events

Firing events is done with `EventController.fire`:
```Java
DeathEvent event = new DeathEvent();
event.character = ...
event.killer = ...

eventController.fire(event, DeathEvent.class, CharacterStateListener.class, CharacterStateListener::onDeath);
```
The following parameters are required:
- an instance of the event to fire
- the class type of the event being fired
- the class type of the listener which should handle the event
- the callback of the listener which is expected to handle it

It is up to the one firing the event to decide what listener callback is supposed to handle it. This will invoked all general listeners of the same the given type. Predicated listener of the same type will be called only if the predicate confirms the call.

A `fire` call can be executed synchronously, asynchronously, be blocking or not, and more. There is no actual specification of the the dispatching is done, and it depends entirely on the implementation. So it is recommended to be aware of the implementation provided.

#### Creating a Controller

The `Controllers` class provides static factory methods for creating `EventController`s of different implementations. Be sure to read the documnetation of each to understand how the dispatching works. Some may require outside dependencies, such as `ExecutorService` customize dispatching. Those dependencies are still managed entirely by the using code, which should close them as needed when done.

## Adding to Project

Releases are deployed to maven cental, so using the library is quite easy.

#### Gradle

For example, adding the __Notifier__ as a dependency to a _Gradle_-based project:
```Groovy
dependencies {
  implementation group: 'com.github.tomtzook', name: 'notifier', version: $version
}
``` 
