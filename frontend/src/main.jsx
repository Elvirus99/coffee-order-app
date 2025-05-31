import React, { useState, useEffect } from 'react'
import ReactDOM from 'react-dom/client'
import './style.css'


function App() {
  const [ime, setIme] = useState('')
  const [broj, setBroj] = useState('')
  const [kolicina, setKolicina] = useState('')
  const [poruka, setPoruka] = useState('')
  const [narudzbe, setNarudzbe] = useState([])

  const fetchNarudzbe = async () => {
    try {
      const res = await fetch('http://localhost:8080/orders')
      const data = await res.json()
      setNarudzbe(data)
    } catch (error) {
      console.error('Greška pri dohvaćanju narudžbi:', error)
    }
  }

  useEffect(() => {
    fetchNarudzbe()
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!/^\d+$/.test(broj)) {
      setPoruka("Unos broja mora sadržavati samo cifre.")
      return
    }
    try {
      const res = await fetch('http://localhost:8080/order', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ime, broj, kolicina: parseInt(kolicina) })
      })
      const data = await res.json()
      setPoruka(data.message)
      setIme('')
      setBroj('')
      setKolicina('')
      fetchNarudzbe()
    } catch (error) {
      setPoruka('Greška pri slanju narudžbe.')
    }
  }

  const obrisiNarudzbu = async (id) => {
    await fetch(`http://localhost:8080/order/${id}`, {
      method: 'DELETE'
    })
    fetchNarudzbe()
  }

  return (
  <div className="container">
    <h1>Naruči </h1>
    <form onSubmit={handleSubmit}>
      <input type="text" placeholder="Ime gosta" value={ime} onChange={(e) => setIme(e.target.value)} required />
      <input type="text" placeholder="Broj stola" value={broj} onChange={(e) => setBroj(e.target.value)} required />
      <input type="number" placeholder="Broj kafa" value={kolicina} onChange={(e) => setKolicina(e.target.value)} required />
      <button type="submit">Pošalji narudžbu</button>
    </form>

    {poruka && <p>{poruka}</p>}

    <h2>Sve narudžbe kafe</h2>
    <ul>
      {narudzbe.map((n, index) => (
        <li key={index} className="narudzba">
          {n.ime} - {n.broj} - {n.kolicina} kom
          <button className="obrisi" onClick={() => obrisiNarudzbu(n.id)}>Obriši</button>
        </li>
      ))}
    </ul>
  </div>
)

}

ReactDOM.createRoot(document.getElementById('root')).render(<App />)
