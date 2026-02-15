import React, { useState, useEffect, useRef } from 'react'
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

import axios from 'axios'

function App() {
  const [username, setUsername] = useState('alice')
  const [token, setToken] = useState(null)
  const [connected, setConnected] = useState(false)
  const [messages, setMessages] = useState([])
  const stompClientRef = useRef(null)

  async function getToken() {
    const res = await axios.get(`/auth/token?username=${encodeURIComponent(username)}`)
    setToken(res.data.token)
  }

  function connect() {
    if (!token) return alert('Get token first');
    const tokenNoBearer = token.replace('Bearer ', '');
    const url = `/ws?token=${encodeURIComponent(tokenNoBearer)}`;
    const client = new Client({
      // Use webSocketFactory so SockJS is used under the hood
      webSocketFactory: () => new SockJS(url),
      connectHeaders: { Authorization: token },
      debug: (str) => { console.log('STOMP DEBUG:', str); },
      onConnect: (frame) => {
        console.log('STOMP connected', frame);
        setConnected(true);

        client.subscribe('/user/queue/notifications', (msg) => {
          console.log('STOMP message received raw:', msg);
          try {
            const body = JSON.parse(msg.body);
            console.log('Parsed message body:', body);
            setMessages(prev => [...prev, body]);
            // ACK
            const ack = { notificationId: body.id, seq: body.seq, toUserId: body.toUserId };
            client.publish({ destination: '/app/ack', body: JSON.stringify(ack) });
          } catch (err) {
            console.error('Failed to parse STOMP message body', err);
          }
        });

        client.publish({ destination: '/app/recover', body: JSON.stringify({ lastSeenSeq: 0 }) });
      },
      onStompError: (err) => { console.error('STOMP error', err); },
      onWebSocketClose: (evt) => { console.log('websocket closed', evt); setConnected(false); }
    });

    client.activate();
    stompClientRef.current = client;
  }


  function sendPrivate() {
    const to = prompt('Send to (username):')
    const content = prompt('Message content:')
    if (!stompClientRef.current) return
    stompClientRef.current.publish({
      destination: '/app/send',
      body: JSON.stringify({ toUserId: to, content })
    })
  }

  async function sendRest() {
    const to = prompt('Send REST to (username)')
    const content = prompt('Message content')
    await axios.post('/notify', { userId: to, message: content })
  }

  return (
    <div style={{ padding: 20 }}>
      <h2>Realtime React Client</h2>
      <div>
        <input value={username} onChange={e => setUsername(e.target.value)} />
        <button onClick={getToken}>Get Token</button>
        <button onClick={connect} disabled={connected}>Connect</button>
        <button onClick={sendPrivate}>Send Private (STOMP)</button>
        <button onClick={sendRest}>Send via REST</button>
      </div>

      <h3>Messages</h3>
      <ul>
        {messages.map((m, i) => (
          <li key={i}><b>ID:</b> {m.id} <b>seq:</b> {m.seq} <pre>{m.payload}</pre></li>
        ))}
      </ul>
    </div>
  )
}

export default App
