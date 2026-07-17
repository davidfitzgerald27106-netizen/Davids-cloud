# MemeCoin — Testnet / Educational Starter

A tiny, honest ERC-20 token you can deploy to a **test network** to learn exactly how
meme coins work — with **no real money and no risk to anyone.**

> ## ⚠️ Read this first
>
> - **This is for learning on a TESTNET.** Test tokens have **zero monetary value**.
> - **Do not launch a real, money-raising token from this** without understanding the
>   serious **legal** (securities law), **financial**, and **ethical** responsibilities
>   involved. Most meme coins lose nearly all their value, and promoting one to an
>   audience can create real legal liability.
> - The deploy script **refuses to deploy to Ethereum mainnet** on purpose.
> - Never put a wallet holding real funds into `.env`. Use a **throwaway test wallet**.

## What this teaches you

- How an ERC-20 token is defined (see [`contracts/MemeCoin.sol`](./contracts/MemeCoin.sol) — it's ~15 real lines).
- Why the contract is written to be **trustworthy**: fixed supply, **no hidden mint**,
  no transfer tax, no blacklist, no pause switch — the opposite of a rug-pull contract.
- How to compile, test, and deploy a contract with Hardhat.

## Prerequisites

- [Node.js](https://nodejs.org) 18+ installed.
- A crypto wallet like [MetaMask](https://metamask.io) — create a **new, empty** account
  just for testing.

## Setup

```bash
cd memecoin
npm install
cp .env.example .env      # then edit .env
```

## Try it locally first (no keys, no internet, instant)

```bash
npm run compile
npm test                  # runs the checks in test/
npm run deploy:local      # deploys to an in-memory chain and prints the address
```

This proves everything works without touching a real network.

## Deploy to the Sepolia testnet

1. **Get a test wallet's private key** from MetaMask (a fresh account). Put it in
   `.env` as `PRIVATE_KEY`.
2. **Get free test ETH** from a Sepolia faucet (e.g.
   [sepoliafaucet.com](https://sepoliafaucet.com) or Alchemy's faucet) — send it to
   that test address.
3. **Get a free RPC URL** from [Alchemy](https://alchemy.com) or
   [Infura](https://infura.io) and put it in `.env` as `SEPOLIA_RPC_URL`.
4. Deploy:
   ```bash
   npm run deploy:sepolia
   ```
5. The script prints a link to view your token on Sepolia Etherscan. You can also
   "import" the token address into MetaMask to see your balance.

## Customizing the token

Edit these in `.env` (no code changes needed):

| Variable | Meaning | Example |
|----------|---------|---------|
| `TOKEN_NAME` | Full name | `Awakening Coin` |
| `TOKEN_SYMBOL` | Ticker | `AWAKE` |
| `TOKEN_SUPPLY` | Whole-token count minted at launch | `1000000` |

## If you ever consider going further

Please talk to a **lawyer** familiar with securities/crypto in your country **before**
launching anything real or accepting any money. "It's just a meme" is not a legal
defense, and the people most likely to get hurt are the ones who trust you. A token
tied to a positivity brand only stays positive if no one loses money they couldn't
afford to lose.
