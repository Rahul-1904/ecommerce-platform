import { useEffect, useState } from 'react'
import client from '../api/client'

const emptyForm = { id: null, name: '', description: '', price: '', stockQuantity: '', categoryId: '' }

export default function Products() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadCategories() {
    const { data } = await client.get('/categories')
    setCategories(data)
  }

  async function loadProducts(pageNum = 0) {
    setLoading(true)
    try {
      const { data } = await client.get('/products', { params: { page: pageNum, size: 10 } })
      setProducts(data.content)
      setTotalPages(data.totalPages)
      setPage(data.number)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load products')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategories()
    loadProducts(0)
  }, [])

  function startEdit(product) {
    setForm({
      id: product.id,
      name: product.name,
      description: product.description || '',
      price: product.price,
      stockQuantity: product.stockQuantity,
      categoryId: product.categoryId,
    })
  }

  function cancelEdit() {
    setForm(emptyForm)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = {
      name: form.name,
      description: form.description,
      price: Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      categoryId: Number(form.categoryId),
    }
    try {
      if (form.id) {
        await client.put(`/products/${form.id}`, payload)
      } else {
        await client.post('/products', payload)
      }
      setForm(emptyForm)
      loadProducts(page)
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed')
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this product?')) return
    setError('')
    try {
      await client.delete(`/products/${id}`)
      loadProducts(page)
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed')
    }
  }

  return (
    <div>
      <h1>Products</h1>

      <form className="grid-form" onSubmit={handleSubmit}>
        <input
          placeholder="Name"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          required
        />
        <input
          placeholder="Description"
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
        <input
          type="number"
          step="0.01"
          min="0.01"
          placeholder="Price"
          value={form.price}
          onChange={(e) => setForm({ ...form, price: e.target.value })}
          required
        />
        <input
          type="number"
          min="0"
          placeholder="Stock qty"
          value={form.stockQuantity}
          onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })}
          required
        />
        <select
          value={form.categoryId}
          onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
          required
        >
          <option value="" disabled>
            Select category…
          </option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <button type="submit">{form.id ? 'Update' : 'Add product'}</button>
        {form.id && (
          <button type="button" className="secondary" onClick={cancelEdit}>
            Cancel
          </button>
        )}
      </form>

      {categories.length === 0 && (
        <div className="hint-banner">
          No categories exist yet — create one on the Categories page first.
        </div>
      )}

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.name}</td>
                  <td>{p.categoryName}</td>
                  <td>₹{p.price}</td>
                  <td>{p.stockQuantity}</td>
                  <td className="actions">
                    <button className="secondary" onClick={() => startEdit(p)}>
                      Edit
                    </button>
                    <button className="danger" onClick={() => handleDelete(p.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && (
                <tr>
                  <td colSpan={6}>No products yet — add one above.</td>
                </tr>
              )}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => loadProducts(page - 1)}>
                Previous
              </button>
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <button disabled={page >= totalPages - 1} onClick={() => loadProducts(page + 1)}>
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
