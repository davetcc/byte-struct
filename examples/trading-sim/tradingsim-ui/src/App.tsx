import {useState} from 'react';
import {BrowserRouter as Router, Route, Routes} from 'react-router-dom'
import {Link} from "react-router";
import logo192Png from './assets/logo192.png'
import './App.css'
import {ShowPricesPage} from "./ShowPricesPage";
import ShowStatisticsPage from "./ShowStatisticsPage";

function Header() {
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const toggleMenu = () => {
        setIsMenuOpen(!isMenuOpen);
    };

    return (
        <header className="app-header">
            <div className="header-main">
                <img style={{width: '60px', height: '60px'}} src={logo192Png} alt="Mock Trading System App Logo"/>
                <h1>TCC Mock Trader</h1>
                <button className="hamburger" onClick={toggleMenu} aria-label="Toggle menu">
                    <span className="bar"></span>
                    <span className="bar"></span>
                    <span className="bar"></span>
                </button>
            </div>
            <ul className={isMenuOpen ? "nav-open" : "nav-closed"}>
                <li><Link to="/" onClick={() => setIsMenuOpen(false)}>Home</Link></li>
                <li><Link to="/prices" onClick={() => setIsMenuOpen(false)}>Prices</Link></li>
                <li><Link to="/stats" onClick={() => setIsMenuOpen(false)}>Stats</Link></li>
            </ul>
        </header>
    )
}

function HomePage() {
    return (
        <section id="center">
            <div>
                <p>
                    This mock trading system is a demonstration of a trading application using React and Spring Boot.
                </p>
            </div>
        </section>
    )
}

function App() {
    return (
        <Router>
            <Header/>
            <Routes>
                <Route path="/" element={<HomePage/>}/>
                <Route path="/prices" element={<ShowPricesPage/>}/>
                <Route path="/stats" element={<ShowStatisticsPage/>}/>
            </Routes>
            <footer>
                Written by TheCodersCorner.com / Dave Cherry. See <a
                href="https://github.com/davetcc/byte-struct">the github repository</a>
            </footer>
        </Router>
    )
}

export default App
