/*
プログラムの実行手順：
1. ターミナル / コマンドプロンプトを開く
2. build.sbt が置かれた場所で sbt と入力し、return を押す
3. project tetris と入力し、return を押す
4. run と入力し、return を押す
5. コンパイルが成功したら、tetris.A を選択（1 と入力）し、return を押す
6. ゲーム画面を閉じたら、手動で java を終了する
7. プログラムを変更後、もう一度実行したいときは run と入力し、return を押す
*/

package tetris

import scala.util.Random

import sgeometry.Pos
import sdraw.{World, Color, Transparent, HSB}

import tetris.{ShapeLib => S}
import java.awt.Shape

// テトリスを動かすための関数
case class TetrisWorld(piece: ((Int, Int), S.Shape), pile: S.Shape) extends World() {

  // マウスクリックは無視
  def click(p: sgeometry.Pos): World = this

  // ブロックの描画
  def drawRect(x: Int, y: Int, w: Int, h: Int, c: Color): Boolean = {
    canvas.drawRect(Pos(A.BlockSize * x, A.BlockSize * y), A.BlockSize * w, A.BlockSize * h, c)
  }

  // shape の描画（与えられた位置）
  def drawShape(pos: (Int, Int), shape: S.Shape): Boolean = {
    val pos_colors = shape.zipWithIndex.flatMap(row_i => {
      val (row, i) = row_i
      row.zipWithIndex.map(box_j => {
        val (color, j) = box_j
        (j, i, color)
      })
    })

    val (x, y) = pos
    pos_colors.forall(pos_color => {
      val (dx, dy, color) = pos_color
      drawRect(x + dx, y + dy, 1, 1, color)
    })
  }

  // shape の描画（原点）
  def drawShape00(shape: S.Shape): Boolean = drawShape((0, 0), shape)
  
  //目的:テトリミノをそのままハードドロップしたときのイメージを表示する。
  def drawGhostBlock(pos: (Int, Int), shape: S.Shape): Boolean = {
    val ghostShape = shape.map(_.map((b: S.Block) => if(b==Transparent) b else sdraw.DarkGray))
    val ((x:Int,y:Int),s:S.Shape) = hardDrop(pos,ghostShape)
    drawShape((x,y),s)
  }

  // ゲーム画面の描画
  val CanvasColor = HSB(0, 0, 0.1f)

  def draw(): Boolean = {
    val (pos, shape) = piece
    canvas.drawRect(Pos(0, 0), canvas.width, canvas.height, CanvasColor) &&
    drawShape00(pile) &&
    drawShape(pos, shape)
    drawGhostBlock(pos, shape)
  }

  // 1, 4, 7. tick
  // 目的：時間の経過に応じて世界を更新する。
  /*  1.
    def tick(): World = {
    val ((x,y), shape) = piece
    TetrisWorld(((x, y+1),shape),  pile)
  } */
  /* 4.
  def tick(): World = {
    val ((x,y), shape) = piece
    if(collision(TetrisWorld(((x, y+1),shape),  pile))) TetrisWorld(piece,  pile)
    else TetrisWorld(((x, y+1),shape),  pile)
  }
  */
  def tick(): World = {
    val ((x,y), shape) = piece
    val NewWorld = TetrisWorld(A.newPiece(), eraseRows(S.combine(S.shiftSE(shape,x,y), pile)))
    if(collision(TetrisWorld(((x, y+1),shape), pile))) if(collision(NewWorld)) TetrisWorld(A.newPiece(), List.fill(A.WellHeight)(List.fill(A.WellWidth)(Transparent))) else NewWorld
    else TetrisWorld(((x, y+1),shape), pile)
  }
  // 2, 5. keyEvent
  // 目的：キー入力に従って世界を更新する。
  /* 2.
  def keyEvent(key: String): World = {
    val ((x,y), shape) = piece
    key match {
      case "RIGHT" => TetrisWorld(((x+1, y),shape),  pile)
      case "LEFT" => TetrisWorld(((x-1, y),shape),  pile)
      case "UP" => TetrisWorld(((x, y),S.rotate(shape)),  pile) 
    }
  }
  */
  /* ハードドロップと特定のツモ練習用のテトリミノ変更ボタン*/
  def keyEvent(key: String): World = {
    val ((x,y), shape) = piece
    val ((nextX, nextY), nextS) = key match {
      case "RIGHT" => ((x+1, y), shape)
      case "LEFT" => ((x-1, y), shape)
      case "UP" => ((x, y), S.rotate(shape))
      case "DOWN" => hardDrop((x,y),shape)
      case "i" => ((A.WellWidth / 2 - 1, 0), S.shapeI)
      case "j" => ((A.WellWidth / 2 - 1, 0), S.shapeJ)
      case "t" => ((A.WellWidth / 2 - 1, 0), S.shapeT)
      case "o" => ((A.WellWidth / 2 - 1, 0), S.shapeO)
      case "z" => ((A.WellWidth / 2 - 1, 0), S.shapeZ)
      case "l" => ((A.WellWidth / 2 - 1, 0), S.shapeL)
      case "s" => ((A.WellWidth / 2 - 1, 0), S.shapeS)
    }
    val NextWorld = TetrisWorld(((nextX, nextY),nextS), pile)
    if(collision(NextWorld)) TetrisWorld(piece, pile) else NextWorld
  }

  // 3. collision
  // 目的：受け取った世界で衝突が起きているかを判定する。
  def collision(world: TetrisWorld): Boolean = {
    val ((x,y), shape) = world.piece
      if(x < 0 || x + S.maxRowLength(shape) > A.WellWidth) true
      else if(y + shape.length > A.WellHeight) true
      else if(S.overlap(S.shiftSE(shape,x,y),world.pile)) true
      else false
  }

  // 6. eraseRows
  // 目的：pile を受け取ったら、揃った行を削除する。
  def eraseRows(pile: S.Shape): S.Shape = {
    def eraseOnly(pile: S.Shape): S.Shape = {
      pile match {
        case Nil => Nil
        case r::rs => if(S.blockCountPerRow(r)==A.WellWidth) eraseOnly(rs) else r::eraseOnly(rs)
      }
    }
  S.empty(A.WellHeight-eraseOnly(pile).length,A.WellWidth) ++ eraseOnly(pile) 
  }

  //目的:テトリミノを可能な限り下に落とした時のpieceを求める。
  def hardDrop(pos: (Int, Int), shape: S.Shape): ((Int,Int),S.Shape) = {
    val ((x,y),s) = (pos,shape)
    if(collision(TetrisWorld(((x, y+1),s), pile))) ((x, y),s)
    else hardDrop((x, y+1),s)
  }
}

// ゲームの実行
object A extends App {
  // ゲームウィンドウとブロックのサイズ
  val WellWidth = 10
  val WellHeight = 10
  val BlockSize = 30

  // 新しいテトロミノの作成
  val r = new Random()

  def newPiece(): ((Int, Int), S.Shape) = {
    val pos = (WellWidth / 2 - 1, 0)
    (pos,
     List.fill(r.nextInt(4))(0).foldLeft(S.random())((shape, _) => shape))
  }

  // 最初のテトロミノ
  val piece = newPiece()

  // ゲームの初期値
  val world = TetrisWorld(piece, List.fill(WellHeight)(List.fill(WellWidth)(Transparent)))

  // ゲームの開始
  world.bigBang(BlockSize * WellWidth, BlockSize * WellHeight, 1)
}
