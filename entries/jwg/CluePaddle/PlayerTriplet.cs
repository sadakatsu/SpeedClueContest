using ClueSharp;

namespace CluePaddle
{
  internal class PlayerTriplet
  {
    internal int m_player;
    internal MurderSet m_set;
    internal bool m_isActive = true;

    public PlayerTriplet(int player, MurderSet triplet)
    {
      m_player = player;
      m_set = triplet;
    }
  }
}