# react-spring

React-spring is a cross-platform spring-physics first animation library for React applications. It provides a powerful, flexible, and performant way to create fluid, natural-feeling animations using spring physics rather than traditional duration-based animations. The library supports multiple platforms including `react-dom`, `react-native`, `react-three-fiber`, `react-konva`, and `react-zdog`.

At its core, react-spring treats animation as a physics problem rather than a time-based one. Springs don't have a defined curve or set duration - they respond naturally to changes, making animations feel more organic and interactive. The library offers both declarative and imperative APIs, allowing developers to choose the approach that best fits their use case. With hooks like `useSpring`, `useSprings`, `useTrail`, and `useTransition`, developers can easily animate single values, lists of items, staggered sequences, or mount/unmount transitions.

## useSpring

The flagship hook for creating spring animations on a single element. It returns animated values that can be applied to `animated` components, automatically interpolating between states using spring physics.

```jsx
import { useSpring, animated } from '@react-spring/web'

// Basic usage with config object - values animate automatically when state changes
function FadeInComponent({ isVisible }) {
  const springs = useSpring({
    opacity: isVisible ? 1 : 0,
    transform: isVisible ? 'translateY(0px)' : 'translateY(-40px)',
    config: { mass: 1, tension: 170, friction: 26 },
  })

  return <animated.div style={springs}>Hello World</animated.div>
}

// With function and imperative API - returns [springs, api] for manual control
function ImperativeExample() {
  const [springs, api] = useSpring(() => ({
    from: { x: 0, y: 0, scale: 1, rotateZ: 0 },
    config: { mass: 5, tension: 350, friction: 40 },
  }))

  const handleClick = () => {
    api.start({
      x: 100,
      y: 50,
      scale: 1.2,
      rotateZ: 45,
    })
  }

  return (
    <animated.div
      onClick={handleClick}
      style={{
        width: 80,
        height: 80,
        background: '#ff6d6d',
        borderRadius: 8,
        ...springs,
      }}
    />
  )
}
```

## useSprings

Creates multiple springs with a unified API, ideal for animating lists of items where each item needs independent animation control.

```jsx
import { useSprings, animated } from '@react-spring/web'
import { useDrag } from 'react-use-gesture'
import clamp from 'lodash.clamp'
import swap from 'lodash-move'

function DraggableList({ items }) {
  const order = useRef(items.map((_, index) => index))

  // Create spring for each item - controls y position, scale, zIndex, and shadow
  const [springs, api] = useSprings(items.length, index => ({
    y: order.current.indexOf(index) * 50,
    scale: 1,
    zIndex: 0,
    shadow: 1,
    immediate: false,
  }))

  const bind = useDrag(({ args: [originalIndex], active, movement: [, y] }) => {
    const curIndex = order.current.indexOf(originalIndex)
    const curRow = clamp(Math.round((curIndex * 50 + y) / 50), 0, items.length - 1)
    const newOrder = swap(order.current, curIndex, curRow)

    // Update all springs based on drag state
    api.start(index =>
      active && index === originalIndex
        ? { y: curIndex * 50 + y, scale: 1.1, zIndex: 1, shadow: 15, immediate: key => key === 'y' || key === 'zIndex' }
        : { y: newOrder.indexOf(index) * 50, scale: 1, zIndex: 0, shadow: 1, immediate: false }
    )

    if (!active) order.current = newOrder
  })

  return (
    <div style={{ height: items.length * 50, position: 'relative' }}>
      {springs.map(({ zIndex, shadow, y, scale }, i) => (
        <animated.div
          {...bind(i)}
          key={i}
          style={{
            position: 'absolute',
            width: '100%',
            zIndex,
            boxShadow: shadow.to(s => `rgba(0, 0, 0, 0.15) 0px ${s}px ${2 * s}px 0px`),
            y,
            scale,
          }}
        >
          {items[i]}
        </animated.div>
      ))}
    </div>
  )
}
```

## useTrail

Creates staggered animations where each spring follows the previous one. Has identical API to `useSprings` but automatically orchestrates springs to animate in sequence.

```jsx
import { useTrail, animated } from '@react-spring/web'
import { useState } from 'react'

function StaggeredText({ open, children }) {
  const items = React.Children.toArray(children)

  // Each item animates after the previous one completes
  const trail = useTrail(items.length, {
    config: { mass: 5, tension: 2000, friction: 200 },
    opacity: open ? 1 : 0,
    x: open ? 0 : 20,
    height: open ? 110 : 0,
    from: { opacity: 0, x: 20, height: 0 },
  })

  return (
    <div>
      {trail.map(({ height, ...style }, index) => (
        <animated.div key={index} style={style}>
          <animated.div style={{ height }}>{items[index]}</animated.div>
        </animated.div>
      ))}
    </div>
  )
}

// Usage
function App() {
  const [open, setOpen] = useState(true)

  return (
    <div onClick={() => setOpen(s => !s)}>
      <StaggeredText open={open}>
        <span>Lorem</span>
        <span>Ipsum</span>
        <span>Dolor</span>
        <span>Sit</span>
      </StaggeredText>
    </div>
  )
}
```

## useTransition

Animates items as they mount, update, and unmount from the DOM. Perfect for lists, notifications, modals, and any content that appears/disappears.

```jsx
import { useTransition, animated } from '@react-spring/web'
import { useState } from 'react'

function NotificationHub({ timeout = 3000 }) {
  const [items, setItems] = useState([])
  const refMap = useMemo(() => new WeakMap(), [])
  const cancelMap = useMemo(() => new WeakMap(), [])

  const transitions = useTransition(items, {
    keys: item => item.key,
    from: { opacity: 0, height: 0, life: '100%' },
    enter: item => async (next, cancel) => {
      cancelMap.set(item, cancel)
      await next({ opacity: 1, height: refMap.get(item).offsetHeight })
      await next({ life: '0%' })
    },
    leave: [{ opacity: 0 }, { height: 0 }],
    onRest: (result, ctrl, item) => {
      setItems(state => state.filter(i => i.key !== item.key))
    },
    config: (item, index, phase) => key =>
      phase === 'enter' && key === 'life'
        ? { duration: timeout }
        : { tension: 125, friction: 20, precision: 0.1 },
  })

  const addNotification = msg => {
    setItems(state => [...state, { key: Date.now(), msg }])
  }

  return (
    <div>
      {transitions(({ life, ...style }, item) => (
        <animated.div style={style}>
          <div ref={ref => ref && refMap.set(item, ref)}>
            <div style={{ right: life }} /> {/* Progress bar */}
            <p>{item.msg}</p>
            <button onClick={() => cancelMap.get(item)?.()}>X</button>
          </div>
        </animated.div>
      ))}
    </div>
  )
}
```

## useChain

Orchestrates multiple animation hooks to run in sequence with configurable timing between each.

```jsx
import { useSpring, useTransition, useChain, useSpringRef, animated } from '@react-spring/web'

function ChainedAnimation({ data, open }) {
  // Create refs to control timing
  const springRef = useSpringRef()
  const transitionRef = useSpringRef()

  // First animation: container grows
  const containerSpring = useSpring({
    ref: springRef,
    from: { size: '20%' },
    to: { size: open ? '100%' : '20%' },
  })

  // Second animation: items fade in
  const transitions = useTransition(open ? data : [], {
    ref: transitionRef,
    from: { opacity: 0, scale: 0 },
    enter: { opacity: 1, scale: 1 },
    leave: { opacity: 0, scale: 0 },
    trail: 400 / data.length,
  })

  // Chain: springRef runs first, transitionRef runs 0.1s after (at 10% of 1000ms timeframe)
  useChain(open ? [springRef, transitionRef] : [transitionRef, springRef], [0, 0.1])

  return (
    <animated.div style={{ width: containerSpring.size, height: containerSpring.size }}>
      {transitions((style, item) => (
        <animated.div style={style}>{item}</animated.div>
      ))}
    </animated.div>
  )
}
```

## useSpringValue

Creates a single animated value with imperative control. Unlike `useSpring`, it does not react to prop changes - updates must be made via methods.

```jsx
import { useSpringValue, animated } from '@react-spring/web'

function MacOSDockIcon({ mouseX, baseWidth, distanceLimit, distanceInput, sizeOutput }) {
  const width = useSpringValue(baseWidth, {
    config: { mass: 0.1, tension: 320, friction: 20 },
  })

  useEffect(() => {
    // Calculate size based on mouse distance
    const distance = Math.abs(mouseX - iconPosition)
    const newWidth = interpolate(distance, distanceInput, sizeOutput)
    width.start(newWidth)
  }, [mouseX])

  return (
    <animated.div style={{ width, height: width }}>
      <img src="/icon.png" />
    </animated.div>
  )
}

// Update value imperatively on interaction
function ClickToAnimate() {
  const opacity = useSpringValue(0, {
    config: { mass: 2, friction: 5, tension: 80 },
  })

  const handleClick = () => opacity.start(1)

  return (
    <animated.div onClick={handleClick} style={{ opacity }}>
      Click to fade in
    </animated.div>
  )
}
```

## Interpolation with to()

Transform spring values into different formats using the `to` method or function. Supports range mapping, chaining, and combining multiple values.

```jsx
import { useSpring, animated, to } from '@react-spring/web'

function Card3D() {
  const [{ x, y, rotateX, rotateY, scale, zoom }, api] = useSpring(() => ({
    rotateX: 0,
    rotateY: 0,
    scale: 1,
    zoom: 0,
    x: 0,
    y: 0,
    config: { mass: 5, tension: 350, friction: 40 },
  }))

  const handleMouseMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect()
    api.start({
      rotateX: -(e.clientY - rect.top - rect.height / 2) / 20,
      rotateY: (e.clientX - rect.left - rect.width / 2) / 20,
      scale: 1.1,
    })
  }

  return (
    <animated.div
      onMouseMove={handleMouseMove}
      onMouseLeave={() => api.start({ rotateX: 0, rotateY: 0, scale: 1 })}
      style={{
        transform: 'perspective(600px)',
        x,
        y,
        // Combine scale and zoom into one transform value
        scale: to([scale, zoom], (s, z) => s + z),
        rotateX,
        rotateY,
      }}
    />
  )
}

// Range-based interpolation with chaining
function RangeInterpolation() {
  const { progress } = useSpring({
    from: { progress: 0 },
    to: { progress: 1 },
  })

  return (
    <animated.div
      style={{
        // Map 0-1 to 0-360, then convert to rotation string
        transform: progress
          .to([0, 0.25, 0.5, 0.75, 1], [0, 90, 180, 270, 360])
          .to(value => `rotateZ(${value}deg)`),
      }}
    />
  )
}
```

## Spring Configuration

Configure spring behavior with mass, tension, friction properties or use presets. Supports per-key configuration and duration-based animations with easings.

```jsx
import { useSpring, animated, config, easings } from '@react-spring/web'

// Using spring physics (recommended)
function SpringPhysics() {
  const [springs, api] = useSpring(() => ({
    y: 0,
    config: {
      mass: 5,        // Higher = slower, more momentum
      tension: 120,   // Higher = faster, snappier
      friction: 120,  // Higher = more damping, less bounce
      clamp: false,   // Stop at goal without overshooting
      precision: 0.01 // Animation precision threshold
    },
  }))

  return <animated.div style={springs} />
}

// Using presets: default, gentle, wobbly, stiff, slow, molasses
function WithPresets() {
  const springs = useSpring({
    from: { scale: 0 },
    to: { scale: 1 },
    config: config.wobbly, // { tension: 180, friction: 12 }
  })

  return <animated.div style={springs} />
}

// Per-key configuration
function PerKeyConfig() {
  const springs = useSpring({
    backgroundColor: '#00ff00',
    y: 100,
    config: key => {
      if (key === 'y') {
        return { mass: 5, friction: 120, tension: 120 }
      }
      return { duration: 1000 } // Duration-based for color
    },
  })

  return <animated.div style={springs} />
}

// Duration-based with easings
function DurationBased() {
  const springs = useSpring({
    from: { x: 0 },
    to: { x: 100 },
    config: {
      duration: 1000,
      easing: easings.easeInOutQuad,
    },
  })

  return <animated.div style={springs} />
}
```

## Async Animations

Chain multiple animation states using arrays or async functions for complex multi-step animations.

```jsx
import { useSpring, animated } from '@react-spring/web'

// Array syntax - each object animates sequentially
function ChainedAnimation() {
  const springs = useSpring({
    from: { background: '#ff6d6d', y: -40, x: 0 },
    to: [
      { x: 80, background: '#fff59a' },
      { y: 40, background: '#88DFAB' },
      { x: 0, background: '#569AFF' },
      { y: -40, background: '#ff6d6d' },
    ],
    loop: true,
  })

  return (
    <animated.div
      style={{
        width: 40,
        height: 40,
        borderRadius: 4,
        ...springs,
      }}
    />
  )
}

// Async function syntax - full control with next() and cancel()
function ScriptedAnimation() {
  const springs = useSpring({
    from: { background: '#ff6d6d', y: -40, x: 0 },
    to: async (next, cancel) => {
      // Can add conditions, delays, loops
      while (true) {
        await next({ x: 80, background: '#fff59a' })
        await next({ y: 40, background: '#88DFAB' })
        await next({ x: 0, background: '#569AFF' })
        await next({ y: -40, background: '#ff6d6d' })
      }
    },
  })

  return <animated.div style={springs} />
}
```

## Parallax Component

Creates scrollable containers with parallax visual effects. Layers move at different speeds based on their `speed` prop.

```jsx
import { Parallax, ParallaxLayer, IParallax } from '@react-spring/parallax'
import { useRef } from 'react'

function ParallaxPage() {
  const parallax = useRef<IParallax>(null)

  const scroll = (to: number) => {
    parallax.current?.scrollTo(to)
  }

  return (
    <Parallax ref={parallax} pages={3} horizontal>
      {/* Background layer - moves slowest */}
      <ParallaxLayer offset={0} speed={0.2}>
        <div className="background" />
      </ParallaxLayer>

      {/* Foreground layer - moves faster */}
      <ParallaxLayer offset={0} speed={0.6} onClick={() => scroll(1)}>
        <div className="foreground" />
      </ParallaxLayer>

      {/* Content layer */}
      <ParallaxLayer offset={0} speed={0.3}>
        <h1>Page 1</h1>
      </ParallaxLayer>

      {/* Sticky layer - stays visible across multiple pages */}
      <ParallaxLayer sticky={{ start: 0, end: 2 }}>
        <nav>Navigation</nav>
      </ParallaxLayer>

      {/* Additional pages */}
      <ParallaxLayer offset={1} speed={0.5}>
        <h1>Page 2</h1>
      </ParallaxLayer>

      <ParallaxLayer offset={2} speed={0.5}>
        <h1>Page 3</h1>
      </ParallaxLayer>
    </Parallax>
  )
}
```

## useScroll

Utility hook for scroll-linked animations. Returns animated values representing scroll position and progress.

```jsx
import { useScroll, animated } from '@react-spring/web'

function ScrollProgress() {
  // scrollYProgress ranges from 0 to 1 as user scrolls
  const { scrollYProgress } = useScroll()

  return (
    <>
      {/* Progress bar */}
      <animated.div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          height: 4,
          background: '#ff6d6d',
          width: scrollYProgress.to(p => `${p * 100}%`),
        }}
      />

      {/* Fade in on scroll */}
      <animated.div style={{ opacity: scrollYProgress }}>
        Content fades in as you scroll
      </animated.div>
    </>
  )
}

// With specific container
function ContainerScroll() {
  const containerRef = useRef(null)
  const { scrollYProgress } = useScroll({ container: containerRef })

  return (
    <div ref={containerRef} style={{ height: 300, overflow: 'auto' }}>
      <animated.div style={{ opacity: scrollYProgress }}>
        Scrollable content
      </animated.div>
    </div>
  )
}
```

## useInView

Triggers animations when elements enter the viewport using IntersectionObserver.

```jsx
import { useInView, animated } from '@react-spring/web'

// Basic boolean check
function VisibilityCheck() {
  const [ref, inView] = useInView()

  return (
    <div ref={ref}>
      {inView ? 'Element is visible!' : 'Scroll to see me'}
    </div>
  )
}

// With spring animation
function AnimateOnScroll() {
  const [ref, springs] = useInView(
    () => ({
      from: { opacity: 0, y: 100 },
      to: { opacity: 1, y: 0 },
    }),
    {
      rootMargin: '-40% 0%',
      once: true, // Only animate once
    }
  )

  return (
    <animated.div ref={ref} style={springs}>
      I fade in when scrolled into view
    </animated.div>
  )
}
```

## useReducedMotion

Respects user's reduced motion preference by automatically skipping animations.

```jsx
import { useReducedMotion, useSpring, animated, Globals } from '@react-spring/web'

// Call at app root to globally respect preference
function App() {
  useReducedMotion() // Sets Globals.skipAnimation automatically

  return <MainContent />
}

// Manual control via Globals
function ManualControl() {
  useEffect(() => {
    Globals.assign({ skipAnimation: true })
    return () => Globals.assign({ skipAnimation: false })
  }, [])

  // Animation will jump to final value immediately
  const springs = useSpring({
    from: { x: 0 },
    to: { x: 100 },
  })

  return <animated.div style={springs} />
}
```

## Event Callbacks

React to animation lifecycle events including start, change, rest, pause, and resume.

```jsx
import { useSpring, animated } from '@react-spring/web'

function AnimationEvents() {
  const [springs, api] = useSpring(() => ({
    x: 0,
    y: 0,

    // Called when animation begins (after first tick)
    onStart: (result, spring) => {
      console.log('Animation started', result.value)
    },

    // Called every frame
    onChange: (result, spring) => {
      console.log('Current value:', result.value)
    },

    // Called when animation comes to rest
    onRest: (result, spring) => {
      console.log('Animation finished', { finished: result.finished, cancelled: result.cancelled })
    },

    // Per-key events
    onStart: {
      x: () => console.log('x started'),
      y: () => console.log('y started'),
    },
  }))

  return (
    <animated.div
      style={springs}
      onClick={() => api.start({ x: 100, y: 100 })}
    />
  )
}
```

## SpringRef Imperative API

Direct control over animation controllers with methods for starting, stopping, pausing, and updating animations.

```jsx
import { useSpring, useSpringRef, animated } from '@react-spring/web'

function ImperativeControl() {
  const api = useSpringRef()
  const springs = useSpring({
    ref: api,
    from: { x: 0, opacity: 1 },
  })

  return (
    <>
      <animated.div style={springs}>Animated element</animated.div>

      {/* Start animation */}
      <button onClick={() => api.start({ x: 100 })}>Move</button>

      {/* Set value immediately without animation */}
      <button onClick={() => api.set({ x: 0 })}>Reset</button>

      {/* Pause/Resume */}
      <button onClick={() => api.pause()}>Pause</button>
      <button onClick={() => api.resume()}>Resume</button>

      {/* Stop and optionally cancel */}
      <button onClick={() => api.stop()}>Stop</button>
      <button onClick={() => api.stop(true)}>Cancel</button>

      {/* Stop specific keys */}
      <button onClick={() => api.stop(['x'])}>Stop X only</button>
    </>
  )
}
```

## Summary

React-spring is ideal for creating natural, physics-based animations in React applications. Common use cases include: fade-in/out effects, slide transitions, drag-and-drop interfaces, notification systems, modal dialogs, parallax scrolling, gesture-based interactions, list reordering, card flips, and scroll-linked effects. The library excels when animations need to feel responsive and interruptible, such as hover effects that smoothly reverse when the cursor leaves.

Integration typically starts by importing hooks from the target package (`@react-spring/web`, `@react-spring/native`, etc.), wrapping elements with the `animated` higher-order component, and applying spring values to style props. For complex applications, the imperative API with `useSpringRef` provides fine-grained control, while `useChain` enables sophisticated multi-step orchestrations. The library integrates seamlessly with gesture libraries like `react-use-gesture` for interactive animations and supports TypeScript with comprehensive type definitions.
