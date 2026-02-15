// ensure `global` exists for Node-style libs used in browser
if (typeof global === 'undefined') {
  // eslint-disable-next-line no-undef
  window.global = window;
}


import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'

createRoot(document.getElementById('root')).render(<App />)
