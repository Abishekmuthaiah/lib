import React, { useState, useEffect } from 'react';
import { 
  LayoutDashboard, BookOpen, Users, ArrowLeftRight, Receipt, Layers,
  Search, RefreshCw, Play, CheckCircle2, AlertCircle, Clock, Check, 
  Copy, ShieldCheck, ChevronRight, BookMarked, Wallet, Sun, Moon,
  TrendingUp, ArrowUpRight, Filter, AlertTriangle, FileText, CheckCircle
} from 'lucide-react';

export default function App() {
  const [theme, setTheme] = useState('light');
  const [activeTab, setActiveTab] = useState('overview');
  
  // Data states
  const [stats, setStats] = useState({ books: 12, members: 6, totalIssues: 58, activeLoans: 2, totalFines: 390.0, collectedFines: 370.0 });
  const [catalog, setCatalog] = useState([]);
  const [members, setMembers] = useState([]);
  const [issues, setIssues] = useState([]);
  const [fines, setFines] = useState([]);
  const [analytics, setAnalytics] = useState([]);
  
  // Filter & Search states
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  
  // Issue Form state
  const [issueMemberId, setIssueMemberId] = useState('');
  const [issueBookId, setIssueBookId] = useState('');
  const [issueDate, setIssueDate] = useState(new Date().toISOString().split('T')[0]);
  const [issueStatusMsg, setIssueStatusMsg] = useState(null);

  // Return Form state
  const [returnIssueId, setReturnIssueId] = useState('');
  const [returnDate, setReturnDate] = useState(new Date().toISOString().split('T')[0]);
  const [returnStatusMsg, setReturnStatusMsg] = useState(null);

  // Receipt Modal state
  const [receiptModalContent, setReceiptModalContent] = useState(null);
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  useEffect(() => {
    loadAllData();
  }, []);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const loadAllData = async () => {
    setLoading(true);
    try {
      await Promise.all([
        fetchStats(),
        fetchCatalog(),
        fetchMembers(),
        fetchIssues(),
        fetchFines(),
        fetchAnalytics()
      ]);
    } catch (err) {
      console.error("Data load failed", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    const res = await fetch('/api/stats');
    if (res.ok) setStats(await res.json());
  };

  const fetchCatalog = async () => {
    const res = await fetch('/api/catalog');
    if (res.ok) {
      const data = await res.json();
      setCatalog(data);
      if (data.length > 0 && !issueBookId) setIssueBookId(data[0].bookId);
    }
  };

  const fetchMembers = async () => {
    const res = await fetch('/api/members');
    if (res.ok) {
      const data = await res.json();
      setMembers(data);
      if (data.length > 0 && !issueMemberId) setIssueMemberId(data[0].memberId);
    }
  };

  const fetchIssues = async () => {
    const res = await fetch('/api/issues');
    if (res.ok) {
      const data = await res.json();
      setIssues(data);
      const active = data.filter(i => i.status === 'ISSUED');
      if (active.length > 0 && !returnIssueId) setReturnIssueId(active[0].issueId);
    }
  };

  const fetchFines = async () => {
    const res = await fetch('/api/fines');
    if (res.ok) setFines(await res.json());
  };

  const fetchAnalytics = async () => {
    const res = await fetch('/api/analytics');
    if (res.ok) setAnalytics(await res.json());
  };

  const handleIssueSubmit = async (e) => {
    e.preventDefault();
    setIssueStatusMsg({ type: 'info', text: 'Executing atomic transaction (decrementing stock, binding PreparedStatement)...' });
    try {
      const res = await fetch('/api/issue', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ memberId: issueMemberId, bookId: issueBookId, issueDate })
      });
      const data = await res.json();
      if (data.success) {
        setIssueStatusMsg({ type: 'success', text: `[TXN COMMIT] Issue #${data.issueId} confirmed. Due Date: ${data.dueDate}` });
        showToast(`Book issued successfully (Issue #${data.issueId})`);
        loadAllData();
      } else {
        setIssueStatusMsg({ type: 'error', text: `[TXN ROLLBACK] ${data.error}` });
      }
    } catch (err) {
      setIssueStatusMsg({ type: 'error', text: err.message });
    }
  };

  const handleReturnSubmit = async (e) => {
    e.preventDefault();
    setReturnStatusMsg({ type: 'info', text: 'Reconciling return, restocking inventory, & checking overdue fines...' });
    try {
      const res = await fetch('/api/return', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ issueId: returnIssueId, returnDate })
      });
      const data = await res.json();
      if (data.success) {
        setReturnStatusMsg({ 
          type: 'success', 
          text: `[TXN COMMIT] Return processed. Overdue Fine: Rs. ${data.fineAmount.toFixed(2)}` 
        });
        showToast(`Return completed! Overdue fine: Rs. ${data.fineAmount.toFixed(2)}`);
        loadAllData();
        if (data.receipt) {
          setReceiptModalContent(data.receipt);
        }
      } else {
        setReturnStatusMsg({ type: 'error', text: `[TXN ROLLBACK] ${data.error}` });
      }
    } catch (err) {
      setReturnStatusMsg({ type: 'error', text: err.message });
    }
  };

  const handlePayFine = async (fineId) => {
    try {
      const res = await fetch('/api/pay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fineId })
      });
      const data = await res.json();
      if (data.success) {
        showToast(`Fine #${fineId} marked as PAID`);
        loadAllData();
      }
    } catch (err) {
      showToast('Error paying fine: ' + err.message, 'error');
    }
  };

  const handleReseed = async () => {
    if (!window.confirm("Reset database to the exact reference state from Slide 10 & 11?")) return;
    try {
      const res = await fetch('/api/reseed', { method: 'POST' });
      const data = await res.json();
      showToast(data.message || 'Database reset & reseeded!');
      loadAllData();
    } catch (err) {
      showToast('Reset failed: ' + err.message, 'error');
    }
  };

  const triggerSlide10Demo = () => {
    const slide10Receipt = 
`=================================================================
         SAMPLE FINE RECEIPT (ISSUE ID 7)
=================================================================
 * Issue ID / Member : 7 | Priya R (STUDENT)
 * Book Title / ISBN : Effective Java / 9780134685991
 * Issue Date        : 2026-05-27
 * Due Date          : 2026-06-10
 * Return Date       : 2026-06-14
 * Overdue Days      : 4
 * Fine Rate         : Rs. 5.00 / day
 * FINE PAYABLE      : Rs. 20.00 (4 x Rs. 5.00)
 * Payment Status    : UNPAID
=================================================================`;
    setReceiptModalContent(slide10Receipt);
    showToast("Loaded Slide 10 Fine Receipt Modal!");
  };

  const copyReceiptText = () => {
    if (receiptModalContent) {
      navigator.clipboard.writeText(receiptModalContent);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const categoriesList = ['ALL', 'Computer Science', 'Engineering Mathematics', 'Fiction', 'General Reference', 'Biography'];
  
  const filteredCatalog = catalog.filter(book => {
    const matchesCat = selectedCategory === 'ALL' || book.category === selectedCategory;
    const matchesSearch = !searchQuery || 
      book.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      book.author.toLowerCase().includes(searchQuery.toLowerCase()) ||
      book.isbn.includes(searchQuery);
    return matchesCat && matchesSearch;
  });

  const activeIssuesList = issues.filter(i => i.status === 'ISSUED');

  return (
    <div className="dashboard-container">
      {/* Toast Notification */}
      {toast && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '24px',
          zIndex: 9999,
          backgroundColor: toast.type === 'error' ? '#ef4444' : '#059669',
          color: '#ffffff',
          padding: '12px 20px',
          borderRadius: '10px',
          boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontWeight: 600,
          fontSize: '0.88rem',
          animation: 'fadeIn 0.2s ease-out'
        }}>
          {toast.type === 'error' ? <AlertCircle size={18} /> : <CheckCircle2 size={18} />}
          {toast.message}
        </div>
      )}

      {/* 1. LEFT SIDEBAR */}
      <aside className="dashboard-sidebar">
        {/* Brand Header */}
        <div className="sidebar-header">
          <div style={{
            width: '42px',
            height: '42px',
            background: 'linear-gradient(135deg, #4f46e5, #6366f1)',
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.3)'
          }}>
            <BookMarked size={24} color="#ffffff" />
          </div>
          <div style={{ overflow: 'hidden' }}>
            <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--text-primary)', letterSpacing: '-0.02em', whiteSpace: 'nowrap' }}>
              DLMS Portal
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
              Evoriea Infotech Pvt. Ltd.
            </div>
          </div>
        </div>

        {/* Sidebar Navigation Links */}
        <nav className="sidebar-nav">
          <div style={{ padding: '6px 12px', fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Main Menu
          </div>

          <button 
            className={`nav-item ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            <LayoutDashboard size={18} />
            Dashboard Overview
          </button>

          <button 
            className={`nav-item ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            <TrendingUp size={18} />
            SQL Analytics & Insights
          </button>

          <button 
            className={`nav-item ${activeTab === 'catalog' ? 'active' : ''}`}
            onClick={() => setActiveTab('catalog')}
          >
            <BookOpen size={18} />
            Book Catalogue
            <span className="nav-badge">{catalog.length}</span>
          </button>

          <button 
            className={`nav-item ${activeTab === 'members' ? 'active' : ''}`}
            onClick={() => setActiveTab('members')}
          >
            <Users size={18} />
            Members Registry
            <span className="nav-badge">{members.length}</span>
          </button>

          <div style={{ padding: '14px 12px 6px 12px', fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Operations & Audit
          </div>

          <button 
            className={`nav-item ${activeTab === 'circulation' ? 'active' : ''}`}
            onClick={() => setActiveTab('circulation')}
          >
            <ArrowLeftRight size={18} />
            Issue / Return Console
            <span className="nav-badge" style={{ backgroundColor: '#fef3c7', color: '#b45309' }}>
              {activeIssuesList.length} active
            </span>
          </button>

          <button 
            className={`nav-item ${activeTab === 'fines' ? 'active' : ''}`}
            onClick={() => setActiveTab('fines')}
          >
            <Receipt size={18} />
            Fine Audit Trail
            <span className="nav-badge">{fines.length}</span>
          </button>

          <button 
            className={`nav-item ${activeTab === 'architecture' ? 'active' : ''}`}
            onClick={() => setActiveTab('architecture')}
          >
            <Layers size={18} />
            System Architecture
          </button>
        </nav>

        {/* Sidebar Footer User Card */}
        <div className="sidebar-footer">
          <div style={{
            background: 'var(--bg-subtle)',
            borderRadius: '12px',
            padding: '12px',
            display: 'flex',
            flexDirection: 'column',
            gap: '4px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: 'linear-gradient(135deg, #0284c7, #06b6d4)',
                color: 'white',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '0.8rem'
              }}>
                AM
              </div>
              <div style={{ overflow: 'hidden' }}>
                <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)', whiteSpace: 'nowrap' }}>
                  Abishek Muthaiah S K
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                  Reg: 95192301003
                </div>
              </div>
            </div>
            <div style={{ fontSize: '0.7rem', color: '#4f46e5', fontWeight: 600, marginTop: '4px' }}>
              P.S.R. Engineering College (CSE)
            </div>
          </div>
        </div>
      </aside>

      {/* 2. MAIN DASHBOARD CONTENT */}
      <div className="dashboard-main">
        {/* Top Header Bar */}
        <header className="dashboard-topbar">
          {/* Search Box */}
          <div style={{ position: 'relative', width: '360px' }}>
            <Search size={16} color="var(--text-subtle)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
            <input
              type="text"
              placeholder="Search catalogue, members, ISBN..."
              className="dash-input"
              style={{ paddingLeft: '38px', backgroundColor: 'var(--bg-subtle)', border: 'none' }}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {/* Action Tools */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {/* Reset Database */}
            <button
              onClick={handleReseed}
              className="dash-btn dash-btn-secondary"
              style={{ padding: '8px 12px', fontSize: '0.8rem' }}
              title="Reset database to reference state"
            >
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
              Reset
            </button>

            {/* Theme Toggle */}
            <button
              onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
              className="dash-btn dash-btn-secondary"
              style={{ padding: '8px 12px', fontSize: '0.8rem' }}
              title="Toggle Light/Dark Theme"
            >
              {theme === 'light' ? <Moon size={15} /> : <Sun size={15} />}
            </button>

            {/* Connection Status Tag */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '6px 12px',
              borderRadius: '999px',
              backgroundColor: '#ecfdf5',
              border: '1px solid #a7f3d0',
              fontSize: '0.75rem',
              color: '#065f46',
              fontWeight: 600
            }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#10b981' }}></span>
              Java 17 JDBC API (:8080)
            </div>
          </div>
        </header>

        {/* Content Body */}
        <main className="content-body">
          {/* Welcome Dashboard Banner */}
          <div style={{
            background: 'linear-gradient(135deg, #4f46e5, #6366f1)',
            borderRadius: '16px',
            padding: '24px 28px',
            color: '#ffffff',
            marginBottom: '26px',
            boxShadow: '0 4px 20px rgba(79, 70, 229, 0.25)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '16px'
          }}>
            <div>
              <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: 'rgba(255,255,255,0.2)', padding: '3px 10px', borderRadius: '999px', fontSize: '0.74rem', fontWeight: 700, marginBottom: '8px' }}>
                <ShieldCheck size={14} />
                23ICS78 INDUSTRIAL INTERNSHIP PROJECT
              </div>
              <h2 style={{ fontSize: '1.45rem', fontWeight: 800, letterSpacing: '-0.02em', margin: 0 }}>
                Digital Library Management System (DLMS)
              </h2>
              <p style={{ fontSize: '0.86rem', opacity: 0.9, marginTop: '4px' }}>
                Automating book circulation, membership borrowing rules, and overdue fine computation at Evoriea Infotech Pvt. Ltd.
              </p>
            </div>
          </div>

          {/* Top 5 KPI Metrics Cards */}
          <div className="kpi-grid">
            {/* KPI 1 */}
            <div className="kpi-card">
              <div>
                <div className="kpi-label">Catalogue Books</div>
                <div className="kpi-value">{stats.books}</div>
                <div className="kpi-sub">Across 5 academic categories</div>
              </div>
              <div className="kpi-icon-wrap" style={{ backgroundColor: '#eef2ff', color: '#4f46e5' }}>
                <BookOpen size={22} />
              </div>
            </div>

            {/* KPI 2 */}
            <div className="kpi-card">
              <div>
                <div className="kpi-label">Registered Members</div>
                <div className="kpi-value">{stats.members}</div>
                <div className="kpi-sub">Student / Faculty / General</div>
              </div>
              <div className="kpi-icon-wrap" style={{ backgroundColor: '#e0f2fe', color: '#0284c7' }}>
                <Users size={22} />
              </div>
            </div>

            {/* KPI 3 */}
            <div className="kpi-card">
              <div>
                <div className="kpi-label">Active Loans</div>
                <div className="kpi-value">{stats.activeLoans}</div>
                <div className="kpi-sub">Currently circulating</div>
              </div>
              <div className="kpi-icon-wrap" style={{ backgroundColor: '#fef3c7', color: '#d97706' }}>
                <ArrowLeftRight size={22} />
              </div>
            </div>

            {/* KPI 4 */}
            <div className="kpi-card">
              <div>
                <div className="kpi-label">Overdue Fines Assessed</div>
                <div className="kpi-value">Rs. {stats.totalFines.toFixed(2)}</div>
                <div className="kpi-sub">Rate: Rs. 5.00 / day</div>
              </div>
              <div className="kpi-icon-wrap" style={{ backgroundColor: '#fee2e2', color: '#e11d48' }}>
                <Wallet size={22} />
              </div>
            </div>

            {/* KPI 5 */}
            <div className="kpi-card">
              <div>
                <div className="kpi-label">Total Circulation</div>
                <div className="kpi-value">{stats.totalIssues}</div>
                <div className="kpi-sub">Issues & returns logged</div>
              </div>
              <div className="kpi-icon-wrap" style={{ backgroundColor: '#f3e8ff', color: '#7c3aed' }}>
                <TrendingUp size={22} />
              </div>
            </div>
          </div>

          {/* ================================================================ */}
          {/* TAB: OVERVIEW & DASHBOARD HOME */}
          {/* ================================================================ */}
          {(activeTab === 'overview' || activeTab === 'analytics') && (
            <div className="fade-in">
              <div style={{ display: 'grid', gridTemplateColumns: '7fr 5fr', gap: '22px', marginBottom: '26px' }}>
                {/* Left Card: Slide 11 Circulation & Fine Breakdown */}
                <div className="dash-card">
                  <div className="dash-card-header">
                    <div>
                      <div className="dash-card-title">
                        <TrendingUp size={18} color="#4f46e5" />
                        Category-wise Circulation & Fine Collection Analysis
                      </div>
                      <div className="dash-card-subtitle">
                        Circulation and fine collection breakdown across 58 recorded issues
                      </div>
                    </div>
                    <span className="status-pill status-student">SQL Aggregate</span>
                  </div>

                  {/* Chart Progress Bars */}
                  <div style={{ marginBottom: '22px' }}>
                    {analytics.map((item, idx) => {
                      const maxVal = 18;
                      const pct = (item.booksIssued / maxVal) * 100;
                      return (
                        <div key={idx} style={{ marginBottom: '16px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem', marginBottom: '5px' }}>
                            <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{item.category}</span>
                            <span className="font-mono" style={{ fontWeight: 700, color: '#4f46e5' }}>
                              {item.booksIssued} issues ({item.shareOfIssues}%) | Fine: Rs. {item.fineCollected.toFixed(2)}
                            </span>
                          </div>
                          <div style={{
                            height: '10px',
                            backgroundColor: '#f1f5f9',
                            borderRadius: '999px',
                            overflow: 'hidden'
                          }}>
                            <div style={{
                              height: '100%',
                              width: `${pct}%`,
                              background: idx === 0 
                                ? 'linear-gradient(90deg, #4f46e5, #06b6d4)' 
                                : idx === 1 
                                ? 'linear-gradient(90deg, #0284c7, #38bdf8)' 
                                : 'linear-gradient(90deg, #6366f1, #a855f7)',
                              borderRadius: '999px',
                              transition: 'width 0.8s ease'
                            }}></div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Slide 11 Table */}
                  <div className="dash-table-wrap">
                    <table className="dash-table">
                      <thead>
                        <tr>
                          <th>Book Category</th>
                          <th style={{ textAlign: 'right' }}>Books Issued</th>
                          <th style={{ textAlign: 'right' }}>Fine Collected</th>
                          <th style={{ textAlign: 'right' }}>Share of Issues</th>
                        </tr>
                      </thead>
                      <tbody>
                        {analytics.map((row, idx) => (
                          <tr key={idx}>
                            <td style={{ fontWeight: 600 }}>{row.category}</td>
                            <td className="font-mono" style={{ textAlign: 'right', fontWeight: 700, color: '#4f46e5' }}>{row.booksIssued}</td>
                            <td className="font-mono" style={{ textAlign: 'right', color: '#b45309', fontWeight: 600 }}>Rs. {row.fineCollected.toFixed(2)}</td>
                            <td style={{ textAlign: 'right', fontWeight: 700 }}>{row.shareOfIssues.toFixed(1)}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Right Card: Management Insights & Quick Issue */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
                  {/* Management Insights */}
                  <div className="dash-card" style={{ backgroundColor: '#f8fafc', borderColor: '#c7d2fe' }}>
                    <div className="dash-card-title" style={{ color: '#3730a3', marginBottom: '14px' }}>
                      <ShieldCheck size={18} color="#4f46e5" />
                      Management Analytics & Insights
                    </div>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.84rem', color: '#334155' }}>
                      <li style={{ marginBottom: '12px', paddingLeft: '18px', position: 'relative' }}>
                        <span style={{ position: 'absolute', left: 0, color: '#4f46e5', fontWeight: 800 }}>•</span>
                        <strong>Computer Science</strong> is the most borrowed category (<strong>18 of 58 issues, 31.0%</strong>) and also yields the highest fine collection (<strong>Rs. 180.00 of Rs. 390.00</strong>).
                      </li>
                      <li style={{ marginBottom: '12px', paddingLeft: '18px', position: 'relative' }}>
                        <span style={{ position: 'absolute', left: 0, color: '#4f46e5', fontWeight: 800 }}>•</span>
                        <strong>Engineering Mathematics</strong> ranks second (<strong>14 issues, Rs. 110.00</strong>), while <strong>Biography</strong> records the lowest circulation (<strong>6 issues, Rs. 20.00</strong>).
                      </li>
                      <li style={{ paddingLeft: '18px', position: 'relative' }}>
                        <span style={{ position: 'absolute', left: 0, color: '#4f46e5', fontWeight: 800 }}>•</span>
                        Overdue fine engine generates automated receipts at <strong>Rs. 5.00/day</strong> with transactional rollbacks.
                      </li>
                    </ul>
                  </div>

                  {/* Active Loans Quick View */}
                  <div className="dash-card" style={{ flex: 1 }}>
                    <div className="dash-card-header" style={{ marginBottom: '14px' }}>
                      <div className="dash-card-title">
                        <Clock size={16} color="#d97706" />
                        Current Borrowers Active
                      </div>
                      <span className="status-pill status-unpaid">{activeIssuesList.length} Active</span>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      {activeIssuesList.map(issue => (
                        <div key={issue.issueId} style={{
                          padding: '12px',
                          borderRadius: '10px',
                          border: '1px solid var(--border-light)',
                          backgroundColor: 'var(--bg-subtle)',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center'
                        }}>
                          <div>
                            <div style={{ fontWeight: 700, fontSize: '0.86rem', color: 'var(--text-primary)' }}>
                              {issue.bookTitle}
                            </div>
                            <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
                              Borrower: <strong>{issue.memberName}</strong> ({issue.membershipType})
                            </div>
                          </div>
                          <div style={{ textAlign: 'right' }}>
                            <div style={{ fontSize: '0.72rem', color: '#b45309', fontWeight: 700 }}>
                              Due: {issue.dueDate}
                            </div>
                            <button
                              onClick={() => {
                                setReturnIssueId(issue.issueId);
                                setActiveTab('circulation');
                              }}
                              className="dash-btn dash-btn-secondary"
                              style={{ padding: '3px 8px', fontSize: '0.72rem', marginTop: '4px' }}
                            >
                              Return Book
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================ */}
          {/* TAB: BOOK CATALOGUE */}
          {/* ================================================================ */}
          {activeTab === 'catalog' && (
            <div className="fade-in">
              <div className="dash-card">
                <div className="dash-card-header" style={{ flexWrap: 'wrap', gap: '14px' }}>
                  <div>
                    <div className="dash-card-title">
                      <BookOpen size={18} color="#4f46e5" />
                      Library Master Book Catalogue
                    </div>
                    <div className="dash-card-subtitle">
                      12 Books Normalized in 3NF Schema across 5 Academic Categories
                    </div>
                  </div>

                  {/* Filter Pills */}
                  <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                    {categoriesList.map(cat => (
                      <button
                        key={cat}
                        onClick={() => setSelectedCategory(cat)}
                        style={{
                          background: selectedCategory === cat ? 'var(--primary)' : 'var(--bg-subtle)',
                          color: selectedCategory === cat ? '#ffffff' : 'var(--text-secondary)',
                          border: '1px solid ' + (selectedCategory === cat ? 'var(--primary)' : 'var(--border-light)'),
                          padding: '5px 12px',
                          borderRadius: '999px',
                          fontSize: '0.76rem',
                          fontWeight: 600,
                          cursor: 'pointer'
                        }}
                      >
                        {cat}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="dash-table-wrap">
                  <table className="dash-table">
                    <thead>
                      <tr>
                        <th>Book ID</th>
                        <th>ISBN</th>
                        <th>Title</th>
                        <th>Author</th>
                        <th>Publisher</th>
                        <th>Category</th>
                        <th style={{ textAlign: 'center' }}>Available / Total</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredCatalog.map(b => {
                        const isAvailable = b.availableCopies > 0;
                        return (
                          <tr key={b.bookId}>
                            <td className="font-mono" style={{ color: 'var(--text-muted)' }}>#{b.bookId}</td>
                            <td className="font-mono" style={{ fontSize: '0.82rem', fontWeight: 600 }}>{b.isbn}</td>
                            <td style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{b.title}</td>
                            <td>{b.author}</td>
                            <td style={{ color: 'var(--text-muted)' }}>{b.publisher}</td>
                            <td>
                              <span className="status-pill status-student">{b.category}</span>
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <span className="font-mono" style={{
                                padding: '3px 8px',
                                borderRadius: '6px',
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                backgroundColor: isAvailable ? '#ecfdf5' : '#ffe4e6',
                                color: isAvailable ? '#059669' : '#e11d48'
                              }}>
                                {b.availableCopies} / {b.totalCopies} copies
                              </span>
                            </td>
                            <td>
                              <button
                                onClick={() => {
                                  setIssueBookId(b.bookId);
                                  setActiveTab('circulation');
                                }}
                                disabled={!isAvailable}
                                className={`dash-btn ${isAvailable ? 'dash-btn-primary' : 'dash-btn-secondary'}`}
                                style={{ padding: '4px 10px', fontSize: '0.76rem', cursor: isAvailable ? 'pointer' : 'not-allowed' }}
                              >
                                Borrow
                              </button>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================ */}
          {/* TAB: MEMBERS REGISTRY */}
          {/* ================================================================ */}
          {activeTab === 'members' && (
            <div className="fade-in">
              <div className="dash-card">
                <div className="dash-card-header">
                  <div>
                    <div className="dash-card-title">
                      <Users size={18} color="#4f46e5" />
                      Library Members Registry & Borrowing Quotas
                    </div>
                    <div className="dash-card-subtitle">
                      Enforced Limits: STUDENT (3 books, 14d) | FACULTY (5 books, 30d) | GENERAL (2 books, 7d)
                    </div>
                  </div>
                  <span className="status-pill status-student">LibraryService Logic</span>
                </div>

                <div className="dash-table-wrap">
                  <table className="dash-table">
                    <thead>
                      <tr>
                        <th>Member ID</th>
                        <th>Full Name</th>
                        <th>Email</th>
                        <th>Phone</th>
                        <th>Membership Tier</th>
                        <th>Active Loans / Limit</th>
                        <th>Join Date</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {members.map(m => {
                        const tierClass = m.membershipType === 'STUDENT' ? 'status-student' : (m.membershipType === 'FACULTY' ? 'status-faculty' : 'status-general');
                        const isLimitReached = m.activeLoans >= m.maxLimit;
                        return (
                          <tr key={m.memberId}>
                            <td className="font-mono">#{m.memberId}</td>
                            <td style={{ fontWeight: 700 }}>{m.fullName}</td>
                            <td style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>{m.email}</td>
                            <td className="font-mono" style={{ fontSize: '0.82rem' }}>{m.phone}</td>
                            <td><span className={`status-pill ${tierClass}`}>{m.membershipType}</span></td>
                            <td>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span className="font-mono" style={{ fontWeight: 700, color: isLimitReached ? '#e11d48' : '#059669' }}>
                                  {m.activeLoans} / {m.maxLimit}
                                </span>
                                <div style={{ width: '60px', height: '6px', backgroundColor: '#e2e8f0', borderRadius: '999px', overflow: 'hidden' }}>
                                  <div style={{
                                    width: `${(m.activeLoans / m.maxLimit) * 100}%`,
                                    height: '100%',
                                    backgroundColor: isLimitReached ? '#e11d48' : '#059669',
                                    borderRadius: '999px'
                                  }}></div>
                                </div>
                              </div>
                            </td>
                            <td className="font-mono" style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{m.joinDate}</td>
                            <td><span className="status-pill status-returned">{m.status}</span></td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================ */}
          {/* TAB: CIRCULATION CONSOLE (ISSUE & RETURN) */}
          {/* ================================================================ */}
          {activeTab === 'circulation' && (
            <div className="fade-in">
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '22px', marginBottom: '26px' }}>
                {/* Issue Form */}
                <div className="dash-card">
                  <div className="dash-card-header">
                    <div className="dash-card-title">
                      <BookOpen size={18} color="#4f46e5" />
                      Issue Book Transaction
                    </div>
                    <span className="status-pill status-student">conn.commit()</span>
                  </div>

                  <form onSubmit={handleIssueSubmit}>
                    <div style={{ marginBottom: '14px' }}>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '5px' }}>
                        Select Member
                      </label>
                      <select
                        className="dash-input"
                        value={issueMemberId}
                        onChange={(e) => setIssueMemberId(e.target.value)}
                        required
                      >
                        {members.map(m => (
                          <option key={m.memberId} value={m.memberId}>
                            #{m.memberId}: {m.fullName} ({m.membershipType}) — Active Loans: {m.activeLoans}/{m.maxLimit}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div style={{ marginBottom: '14px' }}>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '5px' }}>
                        Select Book
                      </label>
                      <select
                        className="dash-input"
                        value={issueBookId}
                        onChange={(e) => setIssueBookId(e.target.value)}
                        required
                      >
                        {catalog.map(b => (
                          <option key={b.bookId} value={b.bookId} disabled={b.availableCopies <= 0}>
                            #{b.bookId}: {b.title} [Stock: {b.availableCopies}/{b.totalCopies}]
                          </option>
                        ))}
                      </select>
                    </div>

                    <div style={{ marginBottom: '18px' }}>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '5px' }}>
                        Issue Date
                      </label>
                      <input
                        type="date"
                        className="dash-input"
                        value={issueDate}
                        onChange={(e) => setIssueDate(e.target.value)}
                        required
                      />
                    </div>

                    <button
                      type="submit"
                      className="dash-btn dash-btn-primary"
                      style={{ width: '100%', padding: '10px' }}
                    >
                      Confirm Book Issue Transaction
                    </button>
                  </form>

                  {issueStatusMsg && (
                    <div style={{
                      marginTop: '14px',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      backgroundColor: issueStatusMsg.type === 'error' ? '#fee2e2' : '#ecfdf5',
                      color: issueStatusMsg.type === 'error' ? '#b91c1c' : '#047857',
                      border: '1px solid ' + (issueStatusMsg.type === 'error' ? '#fca5a5' : '#a7f3d0')
                    }}>
                      {issueStatusMsg.text}
                    </div>
                  )}
                </div>

                {/* Return Form */}
                <div className="dash-card">
                  <div className="dash-card-header">
                    <div className="dash-card-title">
                      <ArrowLeftRight size={18} color="#059669" />
                      Return Book & Overdue Fine Engine
                    </div>
                    <span className="status-pill status-unpaid">Rs. 5.00 / day</span>
                  </div>

                  <form onSubmit={handleReturnSubmit}>
                    <div style={{ marginBottom: '14px' }}>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '5px' }}>
                        Select Active Circulation Loan
                      </label>
                      <select
                        className="dash-input"
                        value={returnIssueId}
                        onChange={(e) => setReturnIssueId(e.target.value)}
                        required
                      >
                        {activeIssuesList.map(i => (
                          <option key={i.issueId} value={i.issueId}>
                            Issue #{i.issueId}: '{i.bookTitle}' by {i.memberName} (Due: {i.dueDate})
                          </option>
                        ))}
                        {activeIssuesList.length === 0 && (
                          <option value="">No active borrowed loans</option>
                        )}
                      </select>
                    </div>

                    <div style={{ marginBottom: '18px' }}>
                      <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '5px' }}>
                        Return Date
                      </label>
                      <input
                        type="date"
                        className="dash-input"
                        value={returnDate}
                        onChange={(e) => setReturnDate(e.target.value)}
                        required
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={activeIssuesList.length === 0}
                      className="dash-btn dash-btn-success"
                      style={{ width: '100%', padding: '10px' }}
                    >
                      Process Return & Generate Receipt
                    </button>
                  </form>

                  {returnStatusMsg && (
                    <div style={{
                      marginTop: '14px',
                      padding: '10px 14px',
                      borderRadius: '8px',
                      fontSize: '0.82rem',
                      fontWeight: 600,
                      backgroundColor: returnStatusMsg.type === 'error' ? '#fee2e2' : '#ecfdf5',
                      color: returnStatusMsg.type === 'error' ? '#b91c1c' : '#047857',
                      border: '1px solid ' + (returnStatusMsg.type === 'error' ? '#fca5a5' : '#a7f3d0')
                    }}>
                      {returnStatusMsg.text}
                    </div>
                  )}
                </div>
              </div>

              {/* Active Loans Table */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <div className="dash-card-title">
                    <Clock size={18} color="#d97706" />
                    Active Loans Currently Circulating
                  </div>
                  <span className="status-pill status-student">{activeIssuesList.length} In Hands</span>
                </div>

                <div className="dash-table-wrap">
                  <table className="dash-table">
                    <thead>
                      <tr>
                        <th>Issue ID</th>
                        <th>Book Title</th>
                        <th>ISBN</th>
                        <th>Borrower</th>
                        <th>Tier</th>
                        <th>Issued Date</th>
                        <th>Due Date</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {activeIssuesList.map(issue => (
                        <tr key={issue.issueId}>
                          <td className="font-mono">#{issue.issueId}</td>
                          <td style={{ fontWeight: 700 }}>{issue.bookTitle}</td>
                          <td className="font-mono" style={{ fontSize: '0.82rem' }}>{issue.isbn}</td>
                          <td>{issue.memberName}</td>
                          <td><span className="status-pill status-student">{issue.membershipType}</span></td>
                          <td className="font-mono">{issue.issueDate}</td>
                          <td className="font-mono" style={{ color: '#b45309', fontWeight: 700 }}>{issue.dueDate}</td>
                          <td><span className="status-pill status-issued">{issue.status}</span></td>
                          <td>
                            <button
                              onClick={() => {
                                setReturnIssueId(issue.issueId);
                                window.scrollTo({ top: 300, behavior: 'smooth' });
                              }}
                              className="dash-btn dash-btn-secondary"
                              style={{ padding: '3px 8px', fontSize: '0.74rem' }}
                            >
                              Select Return
                            </button>
                          </td>
                        </tr>
                      ))}
                      {activeIssuesList.length === 0 && (
                        <tr>
                          <td colSpan="9" style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                            No active loans in circulation.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================ */}
          {/* TAB: FINE AUDIT TRAIL */}
          {/* ================================================================ */}
          {activeTab === 'fines' && (
            <div className="fade-in">
              <div className="dash-card">
                <div className="dash-card-header">
                  <div>
                    <div className="dash-card-title">
                      <Receipt size={18} color="#e11d48" />
                      Overdue Fine Ledger & Transactional Audit Trail
                    </div>
                    <div className="dash-card-subtitle">
                      Fine Formula: Overdue Days × Rs. 5.00/day logged atomically upon return
                    </div>
                  </div>
                  <span className="status-pill status-unpaid">Audit Trail</span>
                </div>

                <div className="dash-table-wrap">
                  <table className="dash-table">
                    <thead>
                      <tr>
                        <th>Fine ID</th>
                        <th>Issue ID</th>
                        <th>Borrower</th>
                        <th>Book Title</th>
                        <th style={{ textAlign: 'center' }}>Overdue Days</th>
                        <th>Fine Amount</th>
                        <th>Reason</th>
                        <th>Payment Status</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {fines.map(f => {
                        const isPaid = f.paidStatus === 'PAID';
                        return (
                          <tr key={f.fineId}>
                            <td className="font-mono">#{f.fineId}</td>
                            <td className="font-mono">#{f.issueId}</td>
                            <td style={{ fontWeight: 700 }}>{f.memberName}</td>
                            <td>{f.bookTitle}</td>
                            <td style={{ textAlign: 'center' }}>
                              <span className="font-mono" style={{ color: '#e11d48', fontWeight: 700 }}>
                                {f.overdueDays} days
                              </span>
                            </td>
                            <td className="font-mono" style={{ color: '#b45309', fontWeight: 800 }}>
                              Rs. {f.amount.toFixed(2)}
                            </td>
                            <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{f.reason}</td>
                            <td>
                              <span className={`status-pill ${isPaid ? 'status-paid' : 'status-unpaid'}`}>
                                {f.paidStatus}
                              </span>
                            </td>
                            <td>
                              {!isPaid ? (
                                <button
                                  onClick={() => handlePayFine(f.fineId)}
                                  className="dash-btn dash-btn-success"
                                  style={{ padding: '3px 8px', fontSize: '0.74rem' }}
                                >
                                  Pay Now
                                </button>
                              ) : (
                                <span style={{ color: 'var(--text-subtle)', fontSize: '0.78rem' }}>Settled</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ================================================================ */}
          {/* TAB: SYSTEM ARCHITECTURE & 3NF ER SCHEMA */}
          {/* ================================================================ */}
          {activeTab === 'architecture' && (
            <div className="fade-in">
              <div className="dash-card" style={{ marginBottom: '24px' }}>
                <div className="dash-card-title" style={{ marginBottom: '6px' }}>
                  <Layers size={18} color="#4f46e5" />
                  Multi-Tier Layered DAO Architecture (Figure 3.1)
                </div>
                <div className="dash-card-subtitle" style={{ marginBottom: '20px' }}>
                  Enterprise 5-Layer Stack developed during the 30-Day Internship at Evoriea Infotech Pvt. Ltd.
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
                  {[
                    { layer: 'Layer 1: Presentation', tech: 'MainApplication.java & React UI', desc: 'CLI menus and modern SaaS dashboard for interactive circulation and receipt printing.' },
                    { layer: 'Layer 2: Service Logic', tech: 'LibraryService.java', desc: 'Enforces student/faculty loan periods, borrow quotas, and overdue fine math.' },
                    { layer: 'Layer 3: Data Access (DAO)', tech: 'LibraryDAO.java', desc: 'Parameterized PreparedStatement queries, ResultSet mapping, and atomic commit/rollback.' },
                    { layer: 'Layer 4: DB Connectivity', tech: 'DBConnection.java', desc: 'Singleton connection manager supporting SQLite standalone and MySQL enterprise servers.' },
                    { layer: 'Layer 5: Relational Storage', tech: 'library_management.db', desc: 'Normalized 3NF relational schema with foreign key cascades and B-Tree indexes.' }
                  ].map((item, idx) => (
                    <div key={idx} style={{
                      backgroundColor: 'var(--bg-subtle)',
                      padding: '16px',
                      borderRadius: '12px',
                      border: '1px solid var(--border-light)'
                    }}>
                      <div style={{ color: '#4f46e5', fontWeight: 700, fontSize: '0.86rem', marginBottom: '4px' }}>
                        {item.layer}
                      </div>
                      <div className="font-mono" style={{ color: '#0284c7', fontSize: '0.8rem', fontWeight: 600, marginBottom: '6px' }}>
                        {item.tech}
                      </div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                        {item.desc}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* 3NF Schema */}
              <div className="dash-card">
                <div className="dash-card-title" style={{ marginBottom: '6px' }}>
                  <Database size={18} color="#059669" />
                  3NF Relational Database Schema & Entities (Figure 3.2)
                </div>
                <div className="dash-card-subtitle" style={{ marginBottom: '20px' }}>
                  Normalized to Third Normal Form (3NF) ensuring data integrity and zero update anomalies.
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
                  {/* BOOKS */}
                  <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                    <div style={{ fontWeight: 800, color: '#0284c7', marginBottom: '8px', display: 'flex', justifyContent: 'space-between' }}>
                      <span>BOOKS</span>
                      <span className="font-mono" style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>PK: book_id</span>
                    </div>
                    <ul className="font-mono" style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      <li>• book_id [PK]</li>
                      <li>• isbn [UNIQUE]</li>
                      <li>• title [VARCHAR]</li>
                      <li>• author [VARCHAR]</li>
                      <li>• publisher [VARCHAR]</li>
                      <li>• category [VARCHAR]</li>
                      <li>• total_copies [INT]</li>
                      <li>• available_copies [INT]</li>
                    </ul>
                  </div>

                  {/* MEMBERS */}
                  <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                    <div style={{ fontWeight: 800, color: '#4f46e5', marginBottom: '8px', display: 'flex', justifyContent: 'space-between' }}>
                      <span>MEMBERS</span>
                      <span className="font-mono" style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>PK: member_id</span>
                    </div>
                    <ul className="font-mono" style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      <li>• member_id [PK]</li>
                      <li>• full_name [VARCHAR]</li>
                      <li>• email [UNIQUE]</li>
                      <li>• phone [VARCHAR]</li>
                      <li>• membership_type</li>
                      <li>• join_date [DATE]</li>
                      <li>• status [ACTIVE]</li>
                    </ul>
                  </div>

                  {/* BOOK_ISSUES */}
                  <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                    <div style={{ fontWeight: 800, color: '#d97706', marginBottom: '8px', display: 'flex', justifyContent: 'space-between' }}>
                      <span>BOOK_ISSUES</span>
                      <span className="font-mono" style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>PK: issue_id</span>
                    </div>
                    <ul className="font-mono" style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      <li>• issue_id [PK]</li>
                      <li>• book_id [FK -&gt; books]</li>
                      <li>• member_id [FK -&gt; members]</li>
                      <li>• issue_date [DATE]</li>
                      <li>• due_date [DATE]</li>
                      <li>• return_date [DATE]</li>
                      <li>• status [ISSUED/RETURNED]</li>
                    </ul>
                  </div>

                  {/* FINES */}
                  <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border-light)' }}>
                    <div style={{ fontWeight: 800, color: '#e11d48', marginBottom: '8px', display: 'flex', justifyContent: 'space-between' }}>
                      <span>FINES</span>
                      <span className="font-mono" style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>PK: fine_id</span>
                    </div>
                    <ul className="font-mono" style={{ listStyle: 'none', padding: 0, margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      <li>• fine_id [PK]</li>
                      <li>• issue_id [FK -&gt; issues]</li>
                      <li>• member_id [FK -&gt; members]</li>
                      <li>• amount [DECIMAL]</li>
                      <li>• reason [VARCHAR]</li>
                      <li>• paid_status [UNPAID/PAID]</li>
                      <li>• created_at [TIMESTAMP]</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Slide 10 Receipt Modal */}
      {receiptModalContent && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          backgroundColor: 'rgba(15, 23, 42, 0.65)',
          backdropFilter: 'blur(6px)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000,
          animation: 'fadeIn 0.2s ease-out'
        }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '16px',
            width: '90%',
            maxWidth: '580px',
            padding: '28px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
            border: '1px solid #e2e8f0'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#0f172a', fontWeight: 800, fontSize: '1.1rem' }}>
                <CheckCircle2 color="#059669" size={22} />
                Formatted Overdue Fine Receipt
              </div>
              <span className="status-pill status-unpaid">UNPAID RECEIPT</span>
            </div>

            <pre className="font-mono" style={{
              backgroundColor: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: '10px',
              padding: '18px',
              fontSize: '0.84rem',
              color: '#0f172a',
              whiteSpace: 'pre-wrap',
              margin: '16px 0',
              lineHeight: 1.6
            }}>
              {receiptModalContent}
            </pre>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                onClick={copyReceiptText}
                className="dash-btn dash-btn-secondary"
              >
                {copied ? <Check size={16} color="#059669" /> : <Copy size={16} />}
                {copied ? 'Copied to Clipboard!' : 'Copy Receipt'}
              </button>
              <button
                onClick={() => setReceiptModalContent(null)}
                className="dash-btn dash-btn-primary"
              >
                Close Receipt
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
