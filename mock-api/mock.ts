import { server } from './mocks/server'

server.listen()

console.log('Mock server running...')

// Keep connection stay alive
setInterval(() => {}, 8080)