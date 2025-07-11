package dev.emberline.core.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A singleton class for managing the registration, unregistration, and dispatching
 * of events to corresponding event listeners.
 * <p>
 * The {@code EventDispatcher} is designed for use in scenarios where events
 * need to be dispatched to single or multiple listeners.
 * <p>
 * Listeners are expected to implement the {@link EventListener} interface and provide
 * handler methods that are annotated with {@link EventHandler}.
 * <p>
 * Thread safety is not maintained for this implementation, and concurrent access
 * should be managed appropriately case by case. The class is designed to be
 * used in a single-threaded context or with external synchronization when used
 * in a multithreaded environment.
 */
@NotThreadSafe
public final class EventDispatcher {
    // Singleton instance of the EventDispatcher
    private static EventDispatcher instance;

    // Map to hold event handler methods and their corresponding listeners
    private final Map<EventHandlerMethod, List<EventListener>> eventHandlers = new HashMap<>();

    private record EventHandlerMethod(Class<? extends EventObject> eventType, Method method) {
        private EventHandlerMethod {
            if (eventType == null || method == null) {
                throw new IllegalArgumentException("Event type and method cannot be null");
            }
        }

        private EventHandlerMethod(final Method method) {
            this(method.getParameterTypes()[0].asSubclass(EventObject.class), method);
        }
    }

    private EventDispatcher() {
        // Prevent instantiation from outside
    }

    /**
     * Returns the singleton instance of the EventDispatcher.
     * This method is synchronized to ensure thread safety when accessing the singleton instance.
     *
     * @return the singleton instance of EventDispatcher
     */
    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "This is a singleton pattern and the instance is managed internally."
    )
    public static synchronized EventDispatcher getInstance() {
        if (instance == null) {
            instance = new EventDispatcher();
        }
        return instance;
    }

    /**
     * Registers the specified event listener with the dispatcher.
     * The listener must implement the {@link EventListener} interface and have methods
     * annotated with {@link EventHandler} that match the expected signature for event handlers.
     *
     * @param listener the event listener to register. Must not be {@code null}.
     * @throws IllegalArgumentException if the {@code listener} is {@code null}.
     */
    public void registerListener(final EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        for (final Method method : getEventHandlerMethods(listener)) {
            eventHandlers.computeIfAbsent(new EventHandlerMethod(method), k -> new ArrayList<>()).add(listener);
        }
    }

    /**
     * Unregisters the specified event listener from the dispatcher. This removes the listener
     * and its associated event handler methods from the internal registry, preventing the listener
     * from receiving further events.
     *
     * @param listener the event listener to unregister. Must not be {@code null}.
     * @throws IllegalArgumentException if the {@code listener} is {@code null}.
     */
    public void unregisterListener(final EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        for (final Method method : getEventHandlerMethods(listener)) {
            final EventHandlerMethod handlerMethod = new EventHandlerMethod(method);
            final List<EventListener> listeners = eventHandlers.get(handlerMethod);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    eventHandlers.remove(handlerMethod);
                }
            }
        }
    }

    /**
     * Unregisters all event listeners from the dispatcher.
     * This method clears the internal map of event handlers and their associated listeners.
     */
    public void unregisterAllListeners() {
        eventHandlers.clear();
    }


    /**
     * Dispatches the specified event to all registered event listeners that have
     * handlers for the type of event being dispatched. There is no guarantee on the
     * order in which the event handlers are called.
     *
     * @param event the event to dispatch, which must be an instance of {@link EventObject}
     *              or one of its subclasses. Must not be {@code null}.
     *
     * @throws IllegalArgumentException if the event parameter is {@code null}.
     * @throws EventHandlerInvocationException if an error occurs while invoking the event handler method.
     */
    // Suppressing warning for setAccessible(true) usage.
    // Event handlers are supposed to be private because they are invoked by the dispatcher
    // and should not be part of the public API.
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @SuppressFBWarnings(
            value = "DP_DO_INSIDE_DO_PRIVILEGED",
            justification = "From spotbugs.readthedocs.io: java.security.AccessController class, "
                    + "which contains the doPrivileged methods, got deprecated in Java 17 (see JEP 411), "
                    + "and removed in Java 24 (see JEP 486). For this reason, this bug isn't reported in "
                    + "classes targeted Java 17 and above."
    )
    public void dispatchEvent(final EventObject event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        for (final var entry : eventHandlers.entrySet()) {
            if (!entry.getKey().eventType.isAssignableFrom(event.getClass())) {
                continue;
            }
            final Method method = entry.getKey().method;
            method.setAccessible(true);
            final List<EventListener> listeners = entry.getValue();
            listeners.forEach(listener -> {
                try {
                    method.invoke(listener, event);
                } catch (final IllegalAccessException | InvocationTargetException e) {
                    throw new EventHandlerInvocationException("Failed to invoke event handler method: " + method.getName(), e);
                }
            });
        }
    }

    /**
     * Retrieves all valid event handler methods from the given listener.
     * @param listener the event listener from which to retrieve event handler methods
     * @return an array of valid event handler methods
     */
    private Method[] getEventHandlerMethods(final EventListener listener) {
        return Arrays.stream(listener.getClass().getDeclaredMethods())
                .filter(this::isEventHandlerMethod)
                .toArray(Method[]::new);
    }

    /**
     * Determines whether the given method qualifies as a valid event handler method.
     * A method is considered an event handler method if it is annotated with {@link EventHandler}
     * and passes the validation checks defined in {@link EventDispatcher#validateEventHandler}.
     *
     * @param method the method to check for event handler validity
     * @return {@code true} if the method is a valid event handler, {@code false} otherwise
     */
    private boolean isEventHandlerMethod(final Method method) {
        if (!method.isAnnotationPresent(EventHandler.class)) {
            return false;
        }
        // If an invalid event handler method is found, it will throw an exception
        validateEventHandler(method);
        return true;
    }

    /**
     * Validates that a method annotated with {@link EventHandlerMethod} satisfies the required signature
     * for an event handler.
     * <p>
     * Event handlers must have exactly one parameter and that parameter must be of type
     * {@link EventObject} or a subclass of it. Event handlers must also be declared in a
     * class that implements {@link EventListener}.
     * <p>
     * If the validation fails, an {@link InvalidEventHandlerException} is thrown.
     *
     * @param method the method to validate as a potential event handler
     * @throws InvalidEventHandlerException if the method does not comply with the expected
     *                                      signature for event handlers
     */
    private void validateEventHandler(final Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new InvalidEventHandlerException(
                    "Event handler methods must have exactly one parameter, but found: " + parameterTypes.length);
        }
        if (!EventObject.class.isAssignableFrom(parameterTypes[0])) {
            throw new InvalidEventHandlerException(
                    "Event handler methods must accept a parameter of type EventObject or a subclass, but found: "
                            + parameterTypes[0].getName());
        }
        if (!EventListener.class.isAssignableFrom(method.getDeclaringClass())) {
            throw new InvalidEventHandlerException(
                    "Event handler methods must be declared in a class that implements EventListener, but found: "
                            + method.getDeclaringClass().getName());
        }
    }
}
