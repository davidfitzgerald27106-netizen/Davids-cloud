// Deploys MemeCoin to whatever network you pass with --network.
// Usage: npx hardhat run scripts/deploy.js --network sepolia
//
// Token name / symbol / supply are read from environment variables so you can
// tweak them without editing code. Defaults are set in .env.example.

const hre = require("hardhat");

async function main() {
  const name = process.env.TOKEN_NAME || "Awakening Coin";
  const symbol = process.env.TOKEN_SYMBOL || "AWAKE";
  const supply = process.env.TOKEN_SUPPLY || "1000000"; // whole tokens

  const network = hre.network.name;
  if (network === "mainnet" || network === "homestead") {
    throw new Error(
      "Refusing to deploy to Ethereum mainnet from this educational starter. " +
        "Use a testnet like 'sepolia'. Remove this guard only if you fully " +
        "understand the legal and financial responsibilities."
    );
  }

  const [deployer] = await hre.ethers.getSigners();
  console.log(`Network:  ${network}`);
  console.log(`Deployer: ${deployer.address}`);
  console.log(`Token:    ${name} (${symbol}), supply ${supply}`);

  const MemeCoin = await hre.ethers.getContractFactory("MemeCoin");
  const token = await MemeCoin.deploy(name, symbol, supply);
  await token.waitForDeployment();

  const address = await token.getAddress();
  console.log(`\n✅ Deployed MemeCoin to: ${address}`);
  console.log(
    `View it: https://${network === "sepolia" ? "sepolia." : ""}etherscan.io/token/${address}`
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
