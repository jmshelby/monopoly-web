# Monopoly Web

Interactive web interface for Monopoly game simulation and statistical analysis.

## Features

- 🎲 **Single Game Simulation**: Run individual games with detailed transaction logs
- 📊 **Bulk Game Analysis**: Run hundreds of games and analyze statistical patterns
- 📈 **Real-time Statistics**: View winner distributions, transaction patterns, and game metrics
- 🎯 **Comprehensive Analysis**: Match the statistical output of the command-line version

## Development

### Prerequisites

- [Clojure CLI](https://clojure.org/guides/getting_started) 1.11+
- [Java](https://adoptium.net/) 17+
- [Node.js](https://nodejs.org/) 18+

### Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Open browser to http://localhost:8280
```

### Available Scripts

```bash
npm run dev          # Start development server with hot reload
npm run build        # Build for production
npm run build:github # Build for GitHub Pages
npm run clean        # Clean build artifacts
npm run test         # Run tests
npm run lint         # Lint code
npm run serve        # Start shadow-cljs server only
```

## Deployment Options

### 1. GitHub Pages (Automatic)

The project is configured for automatic deployment to GitHub Pages via GitHub Actions:

1. Push to `main` branch
2. GitHub Actions will automatically build and deploy
3. Site will be available at `https://yourusername.github.io/monopoly-web/`

**Setup:**
1. Enable GitHub Pages in repository settings
2. Set source to "GitHub Actions"
3. Push to main branch

### 2. Manual GitHub Pages

```bash
npm run deploy:github
git add docs/
git commit -m "Deploy to GitHub Pages"
git push
```

### 3. Docker Deployment

```bash
# Build image
docker build -t monopoly-web .

# Run container
docker run -p 8080:80 monopoly-web

# Open browser to http://localhost:8080
```

### 4. Static Hosting (Netlify, Vercel, etc.)

```bash
# Build for production
npm run build

# Upload contents of resources/public/ to your hosting provider
```

## Architecture

### Project Structure

```
monopoly-web/
├── src/jmshelby/monopoly_web/    # ClojureScript source
│   ├── core.cljs                 # App initialization
│   ├── events.cljs               # Re-frame events
│   ├── subs.cljs                 # Re-frame subscriptions
│   ├── routes.cljs               # Routing
│   └── views_simple.cljs         # UI components
├── resources/public/             # Static assets
│   ├── index.html               # Main HTML template
│   └── vendor/                  # CSS and static resources
├── .github/workflows/           # CI/CD pipeline
├── docker/                      # Docker configuration
└── docs/                        # GitHub Pages build output
```

### Technology Stack

- **ClojureScript**: Main application language
- **Re-frame**: State management and architecture
- **Reagent**: React wrapper for ClojureScript
- **Shadow-cljs**: Build tool and development environment
- **Bootstrap**: CSS framework
- **GitHub Actions**: CI/CD pipeline

## Game Engine Integration

The web interface integrates with the core Monopoly game engine located in `../monopoly/src`. Key integration points:

- `jmshelby.monopoly.core/rand-game-end-state`: Runs complete games
- Real-time progress tracking for bulk simulations
- Statistical analysis matching command-line version output

## CI/CD Pipeline

The project includes a complete CI/CD pipeline with:

1. **Continuous Integration**:
   - Code linting with clj-kondo
   - Test execution
   - Build verification
   - Build artifact generation

2. **Continuous Deployment**:
   - Automatic deployment to GitHub Pages on main branch
   - Build caching for faster pipelines
   - Artifact upload for debugging

## Performance

### Development
- Hot reload with shadow-cljs
- REPL-driven development
- Source maps for debugging

### Production
- Advanced compilation with Google Closure Compiler
- Asset optimization and minification
- Gzip compression
- Browser caching strategies

## Browser Support

- Chrome 70+
- Firefox 65+
- Safari 12+
- Edge 79+

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Related Projects

- [Monopoly Game Engine](../monopoly/) - Core Clojure game logic and simulation engine
