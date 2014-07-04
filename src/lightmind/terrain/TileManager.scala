package lightmind.terrain

class TileManager {
  var tiles = List[Tile]();

  def createTile(position: (Float, Float), texture: (Int, Int)) = {
    val t = new Tile(position, texture)
    tiles = t :: tiles
    t
  }
}
