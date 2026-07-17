const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("MemeCoin", function () {
  it("mints the full supply to the deployer and nothing hidden", async function () {
    const [deployer] = await ethers.getSigners();
    const supply = 1_000_000n;

    const MemeCoin = await ethers.getContractFactory("MemeCoin");
    const token = await MemeCoin.deploy("Awakening Coin", "AWAKE", supply);
    await token.waitForDeployment();

    const decimals = await token.decimals();
    const expected = supply * 10n ** BigInt(decimals);

    // Name, symbol, and total supply are what we asked for.
    expect(await token.name()).to.equal("Awakening Coin");
    expect(await token.symbol()).to.equal("AWAKE");
    expect(await token.totalSupply()).to.equal(expected);

    // The deployer holds 100% at launch — supply equals the deployer's balance.
    expect(await token.balanceOf(deployer.address)).to.equal(expected);
  });

  it("lets holders transfer tokens normally", async function () {
    const [deployer, alice] = await ethers.getSigners();
    const MemeCoin = await ethers.getContractFactory("MemeCoin");
    const token = await MemeCoin.deploy("Awakening Coin", "AWAKE", 1000n);
    await token.waitForDeployment();

    const amount = 10n * 10n ** BigInt(await token.decimals());
    await token.transfer(alice.address, amount);
    expect(await token.balanceOf(alice.address)).to.equal(amount);
  });
});
