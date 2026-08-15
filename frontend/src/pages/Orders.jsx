import { useEffect, useState } from 'react'
import client from '../api/client'

const STATUSES = ['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED']

export default function Orders() {
  const [myOrders, setMyOrders] = useState([])
  const [lookupId, setLookupId] = useState('')
  const [order, setOrder] = useState(null)
  const [newStatus, setNewStatus] = useState('')
  const [error, setError] = useState('')
  const [statusMsg, setStatusMsg] = useState('')

  async function loadMyOrders() {
    try {
      const { data } = await client.get('/orders')
      setMyOrders(data.content)
    } catch {
      // non-fatal, just leave the list empty
    }
  }

  useEffect(() => {
    loadMyOrders()
  }, [])

  async function handleLookup(e) {
    e.preventDefault()
    setError('')
    setStatusMsg('')
    setOrder(null)
    try {
      const { data } = await client.get(`/orders/${lookupId}`)
      setOrder(data)
      setNewStatus(data.status)
    } catch (err) {
      setError(err.response?.data?.message || 'Order not found')
    }
  }

  async function handleUpdateStatus(e) {
    e.preventDefault()
    setError('')
    setStatusMsg('')
    try {
      const { data } = await client.put(`/orders/${order.id}/status`, { status: newStatus })
      setOrder(data)
      setStatusMsg('Status updated.')
      loadMyOrders()
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed')
    }
  }

  return (
    <div>
      <h1>Orders</h1>
      <p className="hint-banner">
        The backend only exposes "my orders" and "look up by ID" — there is no endpoint to list
        every order across all customers. To manage a specific customer's order, you need its ID
        (e.g. from your own testing, or shared by the customer).
      </p>

      <section>
        <h2>Look up an order</h2>
        <form className="inline-form" onSubmit={handleLookup}>
          <input
            type="number"
            placeholder="Order ID"
            value={lookupId}
            onChange={(e) => setLookupId(e.target.value)}
            required
          />
          <button type="submit">Look up</button>
        </form>

        {error && <div className="error-banner">{error}</div>}
        {statusMsg && <div className="success-banner">{statusMsg}</div>}

        {order && (
          <div className="order-detail">
            <p>
              <strong>Order #{order.id}</strong> — Total: ₹{order.totalAmount} — Placed:{' '}
              {new Date(order.createdAt).toLocaleString()}
            </p>
            <p>Shipping address: {order.shippingAddress}</p>
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Qty</th>
                  <th>Price</th>
                  <th>Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item) => (
                  <tr key={item.productId}>
                    <td>{item.productName}</td>
                    <td>{item.quantity}</td>
                    <td>₹{item.priceAtPurchase}</td>
                    <td>₹{item.subtotal}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <form className="inline-form" onSubmit={handleUpdateStatus}>
              <select value={newStatus} onChange={(e) => setNewStatus(e.target.value)}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
              <button type="submit">Update status</button>
            </form>
          </div>
        )}
      </section>

      <section>
        <h2>My orders</h2>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Status</th>
              <th>Total</th>
              <th>Placed</th>
            </tr>
          </thead>
          <tbody>
            {myOrders.map((o) => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.status}</td>
                <td>₹{o.totalAmount}</td>
                <td>{new Date(o.createdAt).toLocaleString()}</td>
              </tr>
            ))}
            {myOrders.length === 0 && (
              <tr>
                <td colSpan={4}>No orders placed by this account yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  )
}
