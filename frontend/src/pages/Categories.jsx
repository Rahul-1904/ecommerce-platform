import { useEffect, useState } from 'react'
import client from '../api/client'

const emptyForm = { id: null, name: '', description: '' }

export default function Categories() {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadCategories() {
    setLoading(true)
    try {
      const { data } = await client.get('/categories')
      setCategories(data)
      setError('')
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load categories')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCategories()
  }, [])

  function startEdit(category) {
    setForm(category)
  }

  function cancelEdit() {
    setForm(emptyForm)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = { name: form.name, description: form.description }
    try {
      if (form.id) {
        await client.put(`/categories/${form.id}`, payload)
      } else {
        await client.post('/categories', payload)
      }
      setForm(emptyForm)
      loadCategories()
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed')
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this category? Products in it may prevent deletion.')) return
    setError('')
    try {
      await client.delete(`/categories/${id}`)
      loadCategories()
    } catch (err) {
      setError(err.response?.data?.message || 'Delete failed (it may still have products)')
    }
  }

  return (
    <div>
      <h1>Categories</h1>

      <form className="inline-form" onSubmit={handleSubmit}>
        <input
          placeholder="Name"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          required
        />
        <input
          placeholder="Description"
          value={form.description || ''}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
        />
        <button type="submit">{form.id ? 'Update' : 'Add category'}</button>
        {form.id && (
          <button type="button" className="secondary" onClick={cancelEdit}>
            Cancel
          </button>
        )}
      </form>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {categories.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.name}</td>
                <td>{c.description}</td>
                <td className="actions">
                  <button className="secondary" onClick={() => startEdit(c)}>
                    Edit
                  </button>
                  <button className="danger" onClick={() => handleDelete(c.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
            {categories.length === 0 && (
              <tr>
                <td colSpan={4}>No categories yet — add one above.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}
