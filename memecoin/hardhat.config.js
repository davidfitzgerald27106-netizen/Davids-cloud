require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

// RPC URL for the Sepolia test network (get a free one from Alchemy or Infura).
const SEPOLIA_RPC_URL = process.env.SEPOLIA_RPC_URL || "";
// Private key of a TEST wallet only. Never use a wallet that holds real funds.
const PRIVATE_KEY = process.env.PRIVATE_KEY || "";

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.24",
  networks: {
    // The default in-memory network — great for quick local tests, no keys needed.
    hardhat: {},
    // Sepolia public testnet. Requires SEPOLIA_RPC_URL and a funded test PRIVATE_KEY.
    sepolia: {
      url: SEPOLIA_RPC_URL,
      accounts: PRIVATE_KEY ? [PRIVATE_KEY] : [],
    },
  },
  // Optional: lets you verify the source on Etherscan after deploy.
  etherscan: {
    apiKey: process.env.ETHERSCAN_API_KEY || "",
  },
};
